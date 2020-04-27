# Fair-Forest
Extension of the JSAT library based on the "Fair forests: Regularized tree induction to minimize model bias" paper by Raff, Edward, Jared Sylvester, and Steven Mills.

## Citations

This project is based on the following paper:
Raff, Edward, Jared Sylvester, and Steven Mills. "Fair forests: Regularized tree induction to minimize model bias." Proceedings of the 2018 AAAI/ACM Conference on AI, Ethics, and Society. 2018.

It uses the JSAT library developed by Edward Raff under GPL 3. The link to the JSAT github page is [here](https://github.com/EdwardRaff/JSAT).

## Changes

I have altered the impurity score of classifiers so that a fairness constraint is added. I have also altered the arff reader to ignore missing values in datasets.

## Running

Example of a decision tree using the fairness constraint is found under src/main/java/com/examples/jsatexamples folder. This contains the relevant testing files for the Community Crime, Compas, and Credit Card data set. Random shuffles of the data sets are found in the resources folder.

