package jsat.classifiers.linear;

import java.util.List;
import jsat.classifiers.ClassificationDataSet;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import static java.lang.Math.*;
import jsat.linear.IndexValue;

/**
 * This class provides static helper methods that may be useful for various
 * linear models.
 *
 * @author Edward Raff
 */
public class LinearTools
{

    private LinearTools()
    {
    }
    

    /**
     * If the linear model performs logistic regression regularized by &lambda;
     * ||w||<sub>1</sub>, this method computes the smallest value of lambda that
     * produces a weight vector of all zeros.<br>
     * <br>
     * Note, that the value returned depends on the data set size. If being used
     * to initialize the value of &lambda; for cross validation with k-folds,
     * the value (k-1)/k * &lambda; will be closer to the correct value of
     * &lambda; for each CV set.
     *
     * @param cds the data set that the model would be trained from
     * @return the smallest value of &lambda; that should produce all zeros. 
     */
    public static double maxLambdaLogisticL1(ClassificationDataSet cds)
    {
        /**
         * This code was ripped out/modified from NewGLMNET. It follows the
         * strategy laid out in Schmidt, M., Fung, G.,&amp;Rosaless, R. (2009).
         * Optimization Methods for L1-Regularization. Retrieved from
         * http://www.cs.ubc.ca/cgi-bin/tr/2009/TR-2009-19.pdf , where we use
         * the coordinate with the largest magnitude of the gradient
         */
        /**
         * if w=0, then D_part[i] = 0.5 for all i
         */
        final double D_part_i = 0.5;
        final int n = cds.getNumNumericalVars();
        Vec delta_L = new DenseVector(n);
        if(cds.rowMajor())
        {
            List<Vec> X = cds.getDataVectors();
            for (int i = 0; i < X.size(); i++)
            {
                double y_i = cds.getDataPointCategory(i) * 2 - 1;
                Vec x = X.get(i);
                delta_L.mutableAdd(D_part_i * y_i, x);
            }
        }
        else
        {
            for(int j = 0; j < cds.getNumNumericalVars(); j++)
            {
                Vec X_j = cds.getNumericColumn(j);
                for(IndexValue iv : X_j)
                {
                    int i = iv.getIndex();
                    double y_i = cds.getDataPointCategory(i) * 2 - 1;
                    delta_L.increment(j, D_part_i*y_i*iv.getValue());
                }
            }
        }
        return max(abs(delta_L.max()), abs(delta_L.min())) / (cds.size());
    }
    
    /**
     * Many linear classifiers can be phrased in two equivalent forms, that only change the notation for the regularized. These forms are:<br>
     * C <big>&sum;</big><sub>i</sub> &#x2113;(w,x<sub>i</sub>) + &Omega;(w) <br>
     * and<br>
     * 1/N <big>&sum;</big><sub>i</sub> &#x2113;(w,x<sub>i</sub>) + &lambda; &Omega;(w) <br>
     * 
     * This method converts the regularization parameter &lambda; to the form used as C
     * 
     * @param lambda the regularization parameter &lambda;
     * @param N the number of data points in the training set 
     * @return 
     */
    public static double lambda2C(double lambda, double N)
    {
        return 1/(lambda*N);
    }
    
/**
     * Many linear classifiers can be phrased in two equivalent forms, that only change the notation for the regularized. These forms are:<br>
     * C <big>&sum;</big><sub>i</sub> &#x2113;(w,x<sub>i</sub>) + &Omega;(w) <br>
     * and<br>
     * 1/N <big>&sum;</big><sub>i</sub> &#x2113;(w,x<sub>i</sub>) + &lambda; &Omega;(w) <br>
     * 
     * This method converts the regularization parameter C to the form used as &lambda;
     * 
     * @param C the regularization parameter C
     * @param N the number of data points in the training set 
     * @return 
     */
    public static double c2Lambda(double C, double N)
    {
        return 1/(C*N);
    }
    
}
