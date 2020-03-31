
package jsat.distributions.multivariate;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import jsat.DataSet;
import jsat.classifiers.DataPoint;
import jsat.linear.DenseVector;
import jsat.linear.Vec;

/**
 * This interface represents the contract that any continuous multivariate distribution must implement
 * 
 * @author Edward Raff
 */
public interface MultivariateDistribution extends Cloneable, Serializable
{
    /**
     * Computes the log of the probability density function. If the 
     * probability of the input is zero, the log of zero would be 
     * {@link Double#NEGATIVE_INFINITY}. Instead, -{@link Double#MAX_VALUE} is returned. 
     * 
     * @param x the array for the vector the get the log probability of
     * @return the log of the probability. 
     * @throws ArithmeticException if the vector is not the correct length, or the distribution has not yet been set
     */
    default public double logPdf(double... x)
    {
        return logPdf(DenseVector.toDenseVec(x));
    }
    
    /**
     * Computes the log of the probability density function. If the 
     * probability of the input is zero, the log of zero would be 
     * {@link Double#NEGATIVE_INFINITY}. Instead, -{@link Double#MAX_VALUE} is returned. 
     * 
     * @param x the vector the get the log probability of
     * @return the log of the probability. 
     * @throws ArithmeticException if the vector is not the correct length, or the distribution has not yet been set
     */
    public double logPdf(Vec x);
    
    /**
     * Returns the probability of a given vector from this distribution. By definition, 
     * the probability will always be in the range [0, 1]. 
     * 
     * @param x the array of the vector the get the log probability of
     * @return the probability 
     * @throws ArithmeticException if the vector is not the correct length, or the distribution has not yet been set
     */
    default public double pdf(double... x)
    {
        return pdf(DenseVector.toDenseVec(x));
    }
    
    /**
     * Returns the probability of a given vector from this distribution. By definition, 
     * the probability will always be in the range [0, 1]. 
     * 
     * @param x the vector the get the log probability of
     * @return the probability 
     * @throws ArithmeticException if the vector is not the correct length, or the distribution has not yet been set
     */
    default public double pdf(Vec x)
    {
        return Math.exp(logPdf(x));
    }
    
    /**
     * Sets the parameters of the distribution to attempt to fit the given list of vectors.
     * All vectors are assumed to have the same weight. 
     * @param <V> the vector type
     * @param dataSet the list of data points
     * @return <tt>true</tt> if the distribution was fit to the data, or <tt>false</tt> 
     * if the distribution could not be fit to the data set. 
     */
    default public <V extends Vec> boolean setUsingData(List<V> dataSet)
    {
        return setUsingData(dataSet, false);
    }
    
    /**
     * Sets the parameters of the distribution to attempt to fit the given list of vectors.
     * All vectors are assumed to have the same weight. 
     * @param <V> the vector type
     * @param dataSet the list of data points
     * @param parallel {@code true} if the training should be done using
     * multiple-cores, {@code false} for single threaded.
     * @return <tt>true</tt> if the distribution was fit to the data, or <tt>false</tt> 
     * if the distribution could not be fit to the data set. 
     */
    public <V extends Vec> boolean setUsingData(List<V> dataSet, boolean parallel);
    
    /**
     * Sets the parameters of the distribution to attempt to fit the given list of data points. 
     * The {@link DataPoint#getWeight()  weights} of the data points will be used.
     * 
     * @param dataPoints the list of data points to use
     * @return <tt>true</tt> if the distribution was fit to the data, or <tt>false</tt> 
     * if the distribution could not be fit to the data set. 
     */
    default public boolean setUsingDataList(List<DataPoint> dataPoints)
    {
        return setUsingData(dataPoints.stream().map(d->d.getNumericalValues()).collect(Collectors.toList()));
    }
       
    /**
     * Sets the parameters of the distribution to attempt to fit the given list of data points. 
     * The {@link DataPoint#getWeight()  weights} of the data points will be used.
     * 
     * @param dataSet the data set to use
     * @return <tt>true</tt> if the distribution was fit to the data, or <tt>false</tt> 
     * if the distribution could not be fit to the data set. 
     */
    default public boolean setUsingData(DataSet dataSet)
    {
        return setUsingData(dataSet, false);
    }
    
    /**
     * Sets the parameters of the distribution to attempt to fit the given list
     * of data points. The {@link DataPoint#getWeight()  weights} of the data
     * points will be used.
     *
     * @param dataSet the data set to use
     * @param parallel the source of threads for computation
     * @return <tt>true</tt> if the distribution was fit to the data, or
     * <tt>false</tt>
     * if the distribution could not be fit to the data set.
     */
    default public boolean setUsingData(DataSet dataSet, boolean parallel)
    {
        return setUsingData(dataSet.getDataVectors(), parallel);
    }

    public MultivariateDistribution clone();
    
    /**
     * Performs sampling on the current distribution. 
     * @param count the number of iid samples to draw
     * @param rand the source of randomness 
     * @return a list of sample vectors from this distribution 
     */
    public List<Vec> sample(int count, Random rand);
}
