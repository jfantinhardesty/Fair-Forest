
package jsat.distributions;

import jsat.linear.Vec;
import jsat.math.Function;
import jsat.math.rootfinding.RiddersMethod;
import static java.lang.Math.*;
import jsat.math.rootfinding.Zeroin;
/**
 *
 * @author Edward Raff
 */
public class Kolmogorov extends ContinuousDistribution
{

    private static final long serialVersionUID = 7319511918364286930L;

    public Kolmogorov()
    {
    }
    

    @Override
    public double pdf(double x)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double cdf(double x)
    {
        if(x < 0)
            throw new ArithmeticException("Invalid value of x, x must be > 0, not " + x);
        else if(x == 0)
            return 0;
        else if(x >= 5)//By this point, floating point isnt accurate enough to distinguish between 1.0 and the true value.
            return 1;
        
        /* 
         * Uses 2 formulas, see http://en.wikipedia.org/wiki/Kolmogorov%E2%80%93Smirnov_test#Kolmogorov_distribution
         * 
         * Each formula converges very rapidly, interface 3 terms to full 
         * IEEE precision for one or the other - crossover point is 1.18 
         * according to Numerical Recipies, 3rd Edition p(334-335)
         */
        double tmp = 0;
        double x2 = x*x;
        if(x < 1.18)
        {
            
            for(int j = 1; j <= 3; j++ )
                tmp += exp( -pow(2*j-1,2)*PI*PI / (8*x2) );
            
            return sqrt(2*PI)/x *tmp;
        }
        else
        {
//            for(int j = 1; j <= 3; j++ )
//                tmp += exp(-2*j*j*x*x)*pow(-1,j-1);
            
            tmp = exp(-2*x2) + exp(-18*x2) - exp(-8*x2);//In order of 1st, 3rd, and 2nd to reduce chances of cancelation
            
            return 1 - 2*tmp;
        }
        
    }
    
    @Override
    public double invCdf(double p)
    {
//        return RiddersMethod.root(0, 5, fCDF, p, p);
        return Zeroin.root(0.0, 5.0, (x) -> this.cdf(x) - p);
    }

    @Override
    public double min()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double max()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDistributionName()
    {
        return "Kolmogorov";
    }

    @Override
    public String[] getVariables()
    {
        return new String[]{};
    }

    @Override
    public double[] getCurrentVariableValues()
    {
        return new double[]{};
    }

    @Override
    public void setVariable(String var, double value)
    {
        
    }

    @Override
    public ContinuousDistribution clone()
    {
        return new Kolmogorov();
    }

    @Override
    public void setUsingData(Vec data)
    {
        
    }

    @Override
    public double mean()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double median()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double mode()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double variance()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double skewness()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
	@Override
	public int hashCode() {
		return 31;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		return true;
	}
    
}
