package com.examples.jsatexamples;

import java.io.File;
import jsat.io.ARFFLoader;
import jsat.DataSet;
import jsat.classifiers.CategoricalResults;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.Classifier;
import jsat.classifiers.DataPoint;
import jsat.classifiers.trees.RandomForest;
import jsat.classifiers.trees.DecisionTree;

import static java.lang.Math.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jsat.classifiers.trees.TreePruner.PruningMethod;

/**
 * A simple classification example
 */
public class CreditCardDefaultTest
{
    public static void main(String[] args)
    {
        //We specify '3' as the class we would like to make the target class. 
        int label = 3;
        
        // Fair attribute is education
        int fairAttribute = 1;
        
        int numTrials = 50;
        
        Double[] errorRate;
        errorRate = new Double[numTrials];
        
        Double[] discrim;
        discrim = new Double[numTrials];
        
        Double[] normDisp;
        normDisp = new Double[numTrials];
        
        for(int i = 0; i < numTrials; i++)
        {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            File file = new File(classloader.getResource("creditcarddefault" + (i+1) + ".arff").getFile());
            DataSet dataSet = ARFFLoader.loadArffFile(file);
            ClassificationDataSet cDataSet = new ClassificationDataSet(dataSet, label);
        
            List<ClassificationDataSet> ret = cDataSet.split(.75,.25);
            
            ClassificationDataSet cDataTrain = ret.get(0);
            ClassificationDataSet cDataTest = ret.get(1);
                      
            int errors = 0;
            //Classifier classifier = new DecisionTree(6, 1, fairAttribute, PruningMethod.NONE, 0);
            Classifier classifier = new RandomForest(100, 6, fairAttribute);
            classifier.train(cDataTrain, true);
            

            int labelPredict[] = new int[cDataTest.size()];
            int fair_attribute[] = new int[cDataTest.size()];

            for(int j = 0; j < cDataTest.size(); j++)
            {
                DataPoint dataPoint = cDataTest.getDataPoint(j);
                int truth = cDataTest.getDataPointCategory(j);

                CategoricalResults predictionResults = classifier.classify(dataPoint);
                int predicted = predictionResults.mostLikely();

                labelPredict[j] = predicted;
                fair_attribute[j] = cDataTest.getDataPoint(j).getCategoricalValue(fairAttribute);


                if(predicted != truth)
                    errors++;
                //System.out.println( j + "| True Class: " + truth + ", Predicted: " + predicted + ", Confidence: " + predictionResults.getProb(predicted) );
            }
            
            errorRate[i] = (double)errors/cDataTest.size();
            discrim[i] = discrimination(labelPredict, fair_attribute);
            normDisp[i] = normedDisparate(labelPredict, fair_attribute);
            System.out.println(errors + " errors were made, " + errorRate[i] + " error rate" );
            System.out.println("Discrimination for gender " + discrim[i]);
            System.out.println("Normed Disparate for gender " + normDisp[i]);
        }
        
        System.out.println((double)sum(errorRate)/numTrials + " error rate over " + numTrials + " trials");
        System.out.println((double)sum(discrim)/numTrials + " discrimination over " + numTrials + " trials");
        System.out.println((double)sum(normDisp)/numTrials + " normed disparate impact over " + numTrials + " trials");
        
        System.out.println("Min Error " + Collections.min(Arrays.asList(errorRate)));
        System.out.println("Min Discrimination " + Collections.min(Arrays.asList(discrim)));
	System.out.println("Min Normed Disparate " + Collections.min(Arrays.asList(normDisp)));
		
	System.out.println("Max Error " + Collections.max(Arrays.asList(errorRate)));
	System.out.println("Max Discrimination " + Collections.max(Arrays.asList(discrim)));
	System.out.println("Max Normed Disparate " + Collections.max(Arrays.asList(normDisp)));
    }
    
    public static double sum(Double[] arr) 
    { 
         double sum = 0; // initialize sum 
         int i; 
        
         // Iterate through all elements and add them to sum 
         for (i = 0; i < arr.length; i++) 
            sum +=  arr[i]; 
        
         return sum; 
     }
    
    public static int sum(int[] arr) 
    { 
         int sum = 0; // initialize sum 
         int i; 
        
         // Iterate through all elements and add them to sum 
         for (i = 0; i < arr.length; i++) 
            sum +=  arr[i]; 
        
         return sum; 
     }
    
    public static double discrimination(int[] labelPredict, int[] fair_attribute)
    {
        int num_c2 = sum(fair_attribute);
        int num_c1 = fair_attribute.length - num_c2;
        
        int sum_pred_c1 = 0;
        int sum_pred_c2 = 0;
        
        for(int i = 0; i < labelPredict.length; i++)
        {
            if(fair_attribute[i] == 0)
            {
                sum_pred_c1 += labelPredict[i];
            }
            else
            {
                sum_pred_c2 += labelPredict[i];
            }
        }
        
        return abs((double)sum_pred_c1/num_c1 - (double)sum_pred_c2/num_c2);
    }
    
    public static double normedDisparate(int[] labelPredict, int[] fair_attribute)
    {
        int num_c2 = sum(fair_attribute);
        int num_c1 = fair_attribute.length - num_c2;
        
        int sum_pred_c1 = 0;
        int sum_pred_c2 = 0;
        
        for(int i = 0; i < labelPredict.length; i++)
        {
            if(fair_attribute[i] == 0)
            {
                sum_pred_c1 += labelPredict[i];
            }
            else
            {
                sum_pred_c2 += labelPredict[i];
            }
        }
        
        if ((double)sum_pred_c1/num_c1 < (double)sum_pred_c2/num_c2)
        {
            return 1 - (double)((double)sum_pred_c1/num_c1) / ((double)sum_pred_c2/num_c2);
        }
        else
        {
            return 1 - (double)((double)sum_pred_c2/num_c2) / ((double)sum_pred_c1/num_c1);
        }
    }
}

