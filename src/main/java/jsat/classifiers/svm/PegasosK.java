
package jsat.classifiers.svm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import jsat.classifiers.CategoricalResults;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.DataPoint;
import jsat.classifiers.calibration.BinaryScoreClassifier;
import jsat.distributions.kernels.KernelTrick;
import jsat.exceptions.FailedToFitException;
import jsat.exceptions.UntrainedModelException;
import jsat.linear.Vec;
import jsat.parameters.Parameter;
import jsat.parameters.Parameterized;
import jsat.utils.FakeExecutor;
import jsat.utils.ListUtils;
import jsat.utils.SystemInfo;
import jsat.utils.concurrent.AtomicDouble;
import jsat.utils.concurrent.ParallelUtils;
import jsat.utils.random.RandomUtil;

/**
 * Implements the kernelized version of the {@link Pegasos} algorithm for SVMs. 
 * Unlike standard SVM algorithms, this one relies on randomness and has no 
 * guarantee to reach the optimal solution, however it is very fast to train. 
 * Each iteration of the algorithm randomly selects one datapoint to potentially 
 * update the coefficient of. <br>
 * The resulting set of support vectors may be more or less sparse than a normal
 * SVM implementation. <br>
 * Because the Pegasos algorithm is stochastic and the kernelized updates on 
 * errors given regularization, the kernelized version may have more difficulty 
 * with noisy or overlapping class distributions. 
 * <br><br>
 * See: Shalev-Shwartz, S., Singer, Y.,&amp;Srebro, N. (2007). <i>Pegasos : Primal
 * Estimated sub-GrAdient SOlver for SVM</i>. 24th international conference on 
 * Machine learning (pp. 807–814). New York, NY: ACM. 
 * doi:10.1145/1273496.1273598
 * 
 * @author Edward Raff
 */
public class PegasosK extends SupportVectorLearner implements BinaryScoreClassifier, Parameterized
{

	private static final long serialVersionUID = 5405460830472328107L;
	private double regularization;
    private int iterations;

    /**
     * Creates a new kernelized Pegasos SVM solver 
     * 
     * @param regularization the amount of regularization to apply, normally a very small positive value
     * @param iterations the number of update iterations to perform
     * @param kernel the kernel to use
     */
    public PegasosK(double regularization, int iterations, KernelTrick kernel)
    {
        this(regularization, iterations, kernel, CacheMode.NONE);
    }
    
    /**
     * Creates a new kernelized Pegasos SVM solver 
     * 
     * @param regularization the amount of regularization to apply, normally a very small positive value
     * @param iterations the number of update iterations to perform
     * @param kernel the kernel to use
     * @param cacheMode what type of kernel caching to use
     */
    public PegasosK(double regularization, int iterations, KernelTrick kernel, CacheMode cacheMode)
    {
        super(kernel, cacheMode);
        setRegularization(regularization);
        setIterations(iterations);
    }

    /**
     * Sets the number of iterations of the algorithm to perform. Each iteration 
     * may or may not update a single coefficient for a specific data point.
     * 
     * @param iterations the number of learning iterations to perform
     */
    public void setIterations(int iterations)
    {
        this.iterations = iterations;
    }

    /**
     * Returns the number of iterations used during training
     * @return the number of iterations used in training
     */
    public int getIterations()
    {
        return iterations;
    }

    /**
     * Sets the amount of regularization to apply. The regularization must be a 
     * positive value
     * @param regularization the amount of regularization to apply
     */
    public void setRegularization(double regularization)
    {
        if(Double.isNaN(regularization) || Double.isInfinite(regularization) || regularization <= 0)
            throw new ArithmeticException("Regularization must be a positive constant, not " + regularization);
        this.regularization = regularization;
    }

    /**
     * Returns the amount of regularization used
     * @return the amount of regularization used
     */
    public double getRegularization()
    {
        return regularization;
    }
    
    @Override
    public PegasosK clone()
    {
        PegasosK clone = new PegasosK(regularization, iterations, getKernel().clone(), getCacheMode());
        
        if(this.vecs != null)
        {
            clone.vecs = new ArrayList<Vec>(vecs);
            clone.alphas = new double[this.alphas.length];
            for(int i = 0; i < this.vecs.size(); i++)
            {
                clone.vecs.set(i, this.vecs.get(i).clone());
                clone.alphas[i] = this.alphas[i];
            }
        }
        
        return clone;
    }

    @Override
    public CategoricalResults classify(DataPoint data)
    {
        if(alphas == null)
            throw new UntrainedModelException("Model has not been trained");
        
        CategoricalResults cr = new CategoricalResults(2);
        
        double sum = getScore(data);

        //SVM only says yess / no, can not give a percentage
        if(sum > 0)
            cr.setProb(1, 1);
        else
            cr.setProb(0, 1);
        
        return cr;
    }

    @Override
    public double getScore(DataPoint dp)
    {
        return kEvalSum(dp.getNumericalValues());
    }

    /**
     * Does part of the run through the data to compute the predictoin
     */
    private class PredictPart implements Callable<Double>
    {
        int i;
        int start;
        int end;
        int[] sign;

        public PredictPart(int i, int start, int end, int[] sign)
        {
            this.i = i;
            this.start = start;
            this.end = end;
            this.sign = sign;
        }
        
        @Override
        public Double call() throws Exception
        {
            final double sign_i = sign[i];
            double val = 0;
            for(int j = start; j < end; j++)
            {
                if(j == i || alphas[j] == 0)
                    continue;
                val += alphas[j]*sign_i* kEval(i, j);
            }
            return val;
        }
        
    }
    
    @Override
    public void train(ClassificationDataSet dataSet, boolean parallel)
    {
        if (dataSet.getClassSize() != 2)
            throw new FailedToFitException("Pegasos only supports binary classification problems");

        Random rand = RandomUtil.getRandom();
        final int m = dataSet.size();

        alphas = new double[m];
        int[] sign = new int[m];
        vecs = new ArrayList<>(m);
        for (int i = 0; i < dataSet.size(); i++)
        {
            vecs.add(dataSet.getDataPoint(i).getNumericalValues());
            sign[i] = dataSet.getDataPointCategory(i) == 1 ? 1 : -1;
        }

        setCacheMode(getCacheMode());//Initiates the cahce
        for (int t = 1; t <= iterations; t++)
        {

            final int i = rand.nextInt(m);
            final double sign_i = sign[i];
            final AtomicDouble val = new AtomicDouble(0.0);

            ParallelUtils.run(true, m, (start, end) -> 
            {
                double val_local = 0;
                for(int j = start; j < end; j++)
                {
                    if(j == i || alphas[j] == 0)
                        continue;
                    val_local += alphas[j]*sign_i* kEval(i, j);
                }

                val.addAndGet(val_local);
            });
            val.set(val.get() * sign_i / (regularization * t));

            if(val.get() < 1)
                alphas[i]++;

        }

        //Collect the non zero alphas
        int pos = 0;
        for (int i = 0; i < alphas.length; i++)
            if (alphas[i] != 0)
            {
                alphas[pos] = alphas[i] * sign[i];
                ListUtils.swap(vecs, pos, i);
                pos++;
            }

        alphas = Arrays.copyOf(alphas, pos);
        vecs = new ArrayList<>(vecs.subList(0, pos));
        setCacheMode(null);
        setAlphas(alphas);
    }

    @Override
    public boolean supportsWeightedData()
    {
        return false;
    }    
}
