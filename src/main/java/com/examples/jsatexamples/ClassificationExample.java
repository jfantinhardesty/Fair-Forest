/*
 * Copyright (C) 2017 Edward Raff
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import jsat.classifiers.trees.TreePruner.PruningMethod;

/**
 * A simple example where we load up a data set for classification purposes. 
 * 
 * @author Edward Raff
 */
public class ClassificationExample
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File file = new File(classloader.getResource("adultTrain.arff").getFile());
        DataSet dataSet = ARFFLoader.loadArffFile(file);
        
        //We specify '8' as the class we would like to make the target class. 
        ClassificationDataSet cDataSet = new ClassificationDataSet(dataSet, 8);
        
        int errors = 0;
        Classifier classifier = new DecisionTree(Integer.MAX_VALUE, 1, PruningMethod.NONE, 0.00001);
        //Classifier classifier = new RandomForest(100);
        classifier.train(cDataSet, true);
        
        File file2 = new File(classloader.getResource("adultTest.arff").getFile());
        DataSet testDataSet = ARFFLoader.loadArffFile(file2);
        ClassificationDataSet ctestDataSet = new ClassificationDataSet(testDataSet, 8);
        
        int labelPredict[] = new int[ctestDataSet.size()];
        int fair_attribute[] = new int[ctestDataSet.size()];
        
        for(int i = 0; i < ctestDataSet.size(); i++)
        {
            DataPoint dataPoint = ctestDataSet.getDataPoint(i);//It is important not to mix these up, the class has been removed from data points in 'cDataSet' 
            int truth = ctestDataSet.getDataPointCategory(i);//We can grab the true category from the data set
            
            //Categorical Results contains the probability estimates for each possible target class value. 
            //Classifiers that do not support probability estimates will mark its prediction with total confidence. 
            CategoricalResults predictionResults = classifier.classify(dataPoint);
            int predicted = predictionResults.mostLikely();
            
            labelPredict[i] = predicted;
            fair_attribute[i] = ctestDataSet.getDataPoint(i).getCategoricalValue(6);
            
            
            if(predicted != truth)
                errors++;
            System.out.println( i + "| True Class: " + truth + ", Predicted: " + predicted + ", Confidence: " + predictionResults.getProb(predicted) );
        }
        
        System.out.println(errors + " errors were made, " + 100.0*(double)errors/ctestDataSet.size() + "% error rate" );
        System.out.println("Discrimination for gender " + discrimination(labelPredict, fair_attribute));
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