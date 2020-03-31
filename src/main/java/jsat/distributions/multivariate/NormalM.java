
package jsat.distributions.multivariate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jsat.linear.CholeskyDecomposition;
import jsat.linear.DenseVector;
import jsat.linear.Matrix;
import jsat.linear.MatrixStatistics;
import jsat.linear.SingularValueDecomposition;
import jsat.linear.Vec;
import static java.lang.Math.*;
import jsat.linear.IndexValue;

/**
 * Class for the multivariate Normal distribution. It is often called the Multivariate Gaussian distribution. 
 * 
 * @author Edward Raff
 */
public class NormalM extends MultivariateDistributionSkeleton
{

    private static final long serialVersionUID = -7043369396743253382L;
    /**
     * When computing the PDF of some x, part of the equation is only dependent on the covariance matrix. This part is
     * <pre>
     *       -k
     *       --          -1
     *        2          --
     * /  __\             2
     * \2 ||/   (|Sigma|)
     * </pre>
     * where k is the dimension, Sigma is the covariance matrix, and || denotes the determinant. <br>
     * Taking the negative log of this gives
     * <pre>
     *         /  __\
     * (-k) log\2 ||/ - log(|Sigma|)
     * -----------------------------
     *               2
     * </pre>
     * 
     * This can then be added to the log of the x dependent part, which, when exponentiated, gives the correct result of dividing by this term. 
     */
    private double logPDFConst;
    /**
     * When we compute the constant {@link #logPDFConst}, we only need the inverse of the covariance matrix. 
     */
    private Matrix invCovariance;
    private Vec invCov_diag;
    private Vec mean;
    /**
     * Lower triangular cholesky decomposition used for sampling such that L * L<sup>T</sup> = Covariance Matrix
     */
    private Matrix L;
    private Vec L_diag;
    /**
     * The determinant of the covariance matrix. 
     */
    private double log_det;

    public NormalM(Vec mean, Matrix covariance)
    {
        setMeanCovariance(mean, covariance);
    }
    
    public NormalM(Vec mean, Vec diag_covariance)
    {
        this.mean = mean.clone();
        setCovariance(diag_covariance);
    }

    public NormalM()
    {
    }
    
    /**
     * Sets the mean and covariance for this distribution. For an <i>n</i> dimensional distribution,
     * <tt>mean</tt> should be of length <i>n</i> and <tt>covariance</tt> should be an <i>n</i> by <i>n</i> matrix.
     * It is also a requirement that the matrix be symmetric positive definite. 
     * @param mean the mean for the distribution. A copy will be used. 
     * @param covariance the covariance for this distribution. A copy will be used. 
     * @throws ArithmeticException if the <tt>mean</tt> and <tt>covariance</tt> do not agree, or the covariance is not 
     * positive definite. An exception may not be throw for all bad matrices. 
     */
    public void setMeanCovariance(Vec mean, Matrix covariance)
    {
        if(!covariance.isSquare())
            throw new ArithmeticException("Covariance matrix must be square");
        else if(mean.length() != covariance.rows())
            throw new ArithmeticException("The mean vector and matrix must have the same dimension," +
                    mean.length() + " does not match [" + covariance.rows() + ", " + covariance.rows() +"]" );
        //Else, we are good!
        this.mean = mean.clone();
        setCovariance(covariance);
    }
    
    /**
     * Sets the covariance matrix for this matrix. 
     * @param covMatrix set the covariance matrix used for this distribution
     * @throws ArithmeticException if the covariance matrix is not square, 
     * does not agree with the mean, or is not positive definite.  An 
     * exception may not be throw for all bad matrices. 
     */
    public void setCovariance(Matrix covMatrix)
    {
        if(!covMatrix.isSquare())
            throw new ArithmeticException("Covariance matrix must be square");
        else if(covMatrix.rows() != this.mean.length())
            throw new ArithmeticException("Covariance matrix does not agree with the mean");
        
        CholeskyDecomposition cd = new CholeskyDecomposition(covMatrix.clone());
        System.out.println();
        L = cd.getLT();
        L.mutableTranspose();
        log_det = cd.getLogDet();
        
        int k = mean.length();
        if(Double.isNaN(log_det) || log_det < log(1e-10))
        {
            //Numerical unstable or sub rank matrix. Use the SVD to work with the more stable pesudo matrix
            SingularValueDecomposition svd = new SingularValueDecomposition(covMatrix.clone());
            //We need the rank deficient PDF and pesude inverse
            this.logPDFConst = 0.5*log(svd.getPseudoDet()) + svd.getRank()*0.5*log(2*PI);
            this.invCovariance = svd.getPseudoInverse();
        }
        else
        {
            this.logPDFConst = (-k*log(2*PI)-log_det)*0.5;
            this.invCovariance = cd.solve(Matrix.eye(k));
        }
        this.invCov_diag = null;
        this.L_diag = null;
    }
    
    public void setCovariance(Vec cov_diag)
    {
        if(cov_diag.length()!= this.mean.length())
            throw new ArithmeticException("Covariance matrix does not agree with the mean");
        
        int k = mean.length();
        
        log_det = 0;
        for(IndexValue iv : cov_diag)
            log_det += Math.log(iv.getValue());
        L_diag = cov_diag.clone();
        L_diag.applyFunction(Math::sqrt);//Cholesky is L*L' = C, sicne just diag, that means sqrt
        invCov_diag = cov_diag.clone();
        this.logPDFConst = (-k*log(2*PI)-log_det)*0.5;
        this.invCov_diag.applyFunction(f->f > 0 ? 1/f : 0.0);
        this.invCovariance = null;
        this.L = null;
    }

    public Vec getMean() 
    {
        return mean;
    }

    @Override
    public double logPdf(Vec x)
    {
        if(mean == null)
            throw new ArithmeticException("No mean or variance set");
        Vec xMinusMean = x.subtract(mean);
        //Compute the part that is depdentent on x
        double xDependent;
        if(invCov_diag != null)
        {
            xDependent = 0;
            for(IndexValue iv : xMinusMean)
                xDependent += iv.getValue()*iv.getValue()*invCov_diag.get(iv.getIndex());
            xDependent *= -0.5;
        }
        else
            xDependent = xMinusMean.dot(invCovariance.multiply(xMinusMean))*-0.5;
        return logPDFConst + xDependent;
    }
    
    @Override
    public double pdf(Vec x)
    {
        double pdf = exp(logPdf(x));
        if(Double.isInfinite(pdf) || Double.isNaN(pdf))//Ugly numerical error has occured
            return 0;
        return pdf;
    }

    @Override
    public <V extends Vec> boolean setUsingData(List<V> dataSet, boolean parallel)
    {
        Vec origMean = this.mean;
        try
        {
            Vec newMean = MatrixStatistics.meanVector(dataSet);
            Matrix covariance = MatrixStatistics.covarianceMatrix(newMean, dataSet);

            this.mean = newMean;
            setCovariance(covariance);
            return true;
        }
        catch(ArithmeticException ex)
        {
            this.mean = origMean;
            return false;
        }
    }

    @Override
    public NormalM clone()
    {
        NormalM clone = new NormalM();
        if(this.invCovariance != null)
            clone.invCovariance = this.invCovariance.clone();
        if(this.mean != null)
            clone.mean = this.mean.clone();
        clone.logPDFConst = this.logPDFConst;
        return clone;
    }
    
    @Override
    public List<Vec> sample(int count, Random rand)
    {
        List<Vec> samples = new ArrayList<>(count);
        Vec Z = new DenseVector(L == null ? L_diag.length() : L.rows());
        
        for(int i = 0; i < count; i++)
        {
            for(int j = 0; j < Z.length(); j++)
                Z.set(j, rand.nextGaussian());
            Vec sample;
            if(L != null)//full diag
                sample = L.multiply(Z);
            else
                sample = L_diag.pairwiseMultiply(Z);
            sample.mutableAdd(mean);
            samples.add(sample);
        }
        
        return samples;
    }
}
