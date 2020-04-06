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
import java.util.List;
import jsat.classifiers.trees.TreePruner.PruningMethod;

/**
 * A simple classification example
 */
public class CrimeCommunity
{
    
    public static void main(String[] args)
    {
        
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File file = new File(classloader.getResource("crimecommunity.arff").getFile());
        DataSet dataSet = ARFFLoader.loadArffFile(file);
        
        //We specify '1' as the class we would like to make the target class. 
        int label = 1;
        
        // Fair attribute is percentage of African American residents binarized.
        int fairAttribute = 0;
        
        ClassificationDataSet cDataSet = new ClassificationDataSet(dataSet, label);
        
        int numTrials = 50;
        
        int totalNum = dataSet.size();
        int numTrain = (int)(0.75*totalNum);
        
        for(int i = 0; i < numTrials; i++)
        {
            List<ClassificationDataSet> ret = cDataSet.randomSplit(.75,.25);
            
            ClassificationDataSet cDataTrain = ret.get(0);
            ClassificationDataSet cDataTest = ret.get(1);
                      
            int errors = 0;
            //Classifier classifier = new DecisionTree(Integer.MAX_VALUE, 1, fairAttribute, PruningMethod.NONE, 0);
            Classifier classifier = new RandomForest(100, fairAttribute);
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
            
            System.out.println(errors + " errors were made, " + 100.0*(double)errors/cDataTest.size() + "% error rate" );
            System.out.println("Discrimination for gender " + discrimination(labelPredict, fair_attribute));
        }
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
}
