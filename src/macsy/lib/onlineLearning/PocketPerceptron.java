package macsy.lib.onlineLearning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import macsy.lib.AUC;
import macsy.lib.BasicLinearModel;
import macsy.lib.DataPoint;
import macsy.lib.LinearModel;
import macsy.lib.Results;


/**
 * This class implements the Pocket Perceptron algorithm. It is a type of linear classifier, i.e. a
 * classification algorithm that makes its predictions based on a linear predictor function
 * combining a set of weights with the feature vector. An update is made if an error
 * did occur or not. The algorithm saves the best weight vector that has occured so far and uses it
 * iteratively based on parameters such as the number of consecutive correct classification using perceptron
 * and pocket weights and the number of total training examples correctly classified by perceptron
 * and pocket weights.
 *
 * The update rule is as follows:
 * e.g. prediction: <w(t)*x(t)> + b
 * 		if error
 * 			update: w(t+1) = w(t) + eta*(desired_output-real_output)*x(t)
 * 					b(t+1) = b(t) + eta*(desired_output-real_output)
 *
 * where w is the weight vector, x is the input, b is the bias and eta the learning factor
 *  * @author Saatviga Sudhahar
 **/
public class PocketPerceptron implements OnlineLearning {

    public static final double STABLE_DECISION_THRESHOLD = -1;
    // variable for the time (timestamp)
    private long timestamp;
    // variable to store the desired precision the user defines. This is used for
    // the extra bias we use in the algorithm (it is set to -1 if we don't wish extra bias)
    private double desiredPrecision;
    // variable to store the extra bias - r (which is subtracted from the actual output)
    private double threshold_for_precisionANDrecall;
    // variable to change the value of the extra bias (r)
    private double change_of_thres;
    // this is the variable given by the user for the learning factor (eta) used in the
    // updating rule (learning process) w(t+1) = w(t) + eta * y(t) * x(t) (see below)
    private double learningFactor;
    // this is a boolean variable for the user to choose if the learning factor will be
    // updated according to the number of the instances per class (different learning
    // factors per class) or not
    private boolean updateLearningFactor;
    
    // parameters like the window or the alpha for the exponential moving average
    // calculation (we use the formula a = 2/(1+N), where N (expMovingAverage_window) is
    // the number of data we wish to remember and a (expMovingAverage_a) is the smoothing
    // constant used in the rule X(t) = alpha*Y(t) + (1-alpha)*X(t-1), where X is the input and Y
    // the event of interest (e.g. the appearance of error if it's the exponential moving
    // average error, that we compute
    private int expMovingAverage_window;
    private double expMovingAverage_a;
    // variable for storing the error
    private double expMovingAverage_error;
    // boolean variables for the initialise the number of positive or negative instances
    private boolean first_pos;
    private boolean first_neg;
    // counters for the number of positive or negative instances are met
    private double N_hat_Pos = 1.0;
    private double N_hat_Neg = 1.0;
    // Define the positions of positive (1) tag for the tag list
    private static final int POSITIVE_LABEL = 1;
    // matrix to hold statistic information
    private long[] StatisticsMatrix = new long[4];
    // define the positions of TP, TN, FP and FN in the statistics matrix
    private static final int TP = 0;
    private static final int TN = 1;
    private static final int FP = 2;
    private static final int FN = 3;
    // the model used for the algorithm of online learning
    private LinearModel linearModel;
    // initial values for the exponential moving average of the number of pos/neg
    private static double M_pos = 0.0;
    private static double M_neg = 0.0;
    private int N_overall = 0;
    // object to hold the information for the area under the curve
    AUC AUC_object;
    private double AUC_value;
    //counters for pocket perceptron
    //# of consecutive correct classification using perceptron weights
    private int run_p;
    //# of total training examples correctly classified by perceptron weights
    private int num_ok_p;
    private int run_w;
    private int num_ok_w;

    public PocketPerceptron() {
    }

    public PocketPerceptron(String wFileName) throws Exception {
        // Initialise the correct variables

        // load the model if it already exists or create an empty file
        loadModel(wFileName);
        // the threshold for decision is set to 0
        threshold_for_precisionANDrecall = 0.0;
        // the change of the threshold (if necessary) is set to 0.00001
        change_of_thres = 0.00001;
        // these variables are to define the first time the number of instances per class
        // are calculated (for initialisation purposes only)
        first_pos = true;
        first_neg = true;
        // create the AUC object
        AUC_object = new AUC();

    }

    /**
     * This function performs the prediction and the training procedure taking into account
     * the model, of a given input sample
     *
     *  @param sample : the input DataPoint
     *
     *  This function also predicts the output of the sample with the given model
     *  in order to update the model, the error and the statistical information
     */
    @Override
    public void train(DataPoint sample) throws Exception {
        // update the exponential moving average for the number of instances per
        // class
        updateNum(sample);
        double y_hat;
        // predict the output of the input sample using the model (actual
        // output)
        y_hat = linearModel.predict(sample);

        //System.out.println("Predicted value: " + y_hat);

        sample.setPredictedLabel_Value(y_hat);
        sample.setPredictedLabel((y_hat > linearModel.getB()) ? 1 : -1);
        //System.out.println("Bias: " + linearModel.getB());

        // boolean variable to define if there was an error on the prediction or
        // not
        boolean error = false;

        // the predicted value is compared to the threshold
        threshold_for_precisionANDrecall = 0;

        // Compare the prediction to the real label
        if (sample.getPredictedLabel() == sample.getRealLabel()) {
            // Correct prediction
            int T = (sample.getPredictedLabel() == 1) ? TP : TN;
            StatisticsMatrix[T]++;
            //increment the counter for number consecutive correct classifications by perceptron weights
            run_p++;
            //increment the counter for total training examples correctly classified perceptron weights
            num_ok_p++;


        } else {
            // Incorrect prediction
            error = true;
            int F = (sample.getPredictedLabel() == 1) ? FP : FN;
            StatisticsMatrix[F]++;
            //set the counter for the number consecutive correct classifications to 0 since an error occured
            setRun_p(0);
        }
        // if an error or margin error is occurred
        //System.out.println("Error: " + error);
        if (error) {
            // update the model
            learningProcess(sample, y_hat);
            // If precision =-1 then do not adapt
            if (this.desiredPrecision != STABLE_DECISION_THRESHOLD) {
                this.updateThres(this.statsGetPrecision());
            }
        }
        // calculate the error rate (exponential moving error average)
        update_exponentialMovingAverageError(error);

        // update the counter of the negative instances
        update_exponentialMovingAverageCounter(sample.getRealLabel());

        statsGetAUC(sample);


    }

    /**
     * Perform the learning procedure / updates the w, according to the formula
     * w(t+1) = w(t) + eta*(d-y(t))*x(t)
     *
     * where t is the time step, eta is the learning factor, d is the correct / desired output,
     * y is the predicted / actual output and x the input
     *
     * @param X : the input feature vector
     * @param learningFactor : the learning factor of learning
     * @param y_hat : the predicted output
     */
    private void learningProcess(DataPoint X, double y_hat) throws Exception {
        // if the user wishes to use an updating learning factor, different per
        // class
        double weight;
        if (this.updateLearningFactor) // we fix the weights depending on the
        // class of the instance
        // (positive or negative)
        {
            System.out.println("In update learning factor block.");
            if (X.getRealLabel() == POSITIVE_LABEL) {
                weight = learningFactor * N_hat_Neg;
            } else {
                weight = learningFactor * N_hat_Pos;
            }
        } else // if not
        {
            weight = learningFactor;
        }
        //System.out.println("Learning Factor: " + learningFactor);
        int predicted_label = X.getPredictedLabel();
        int real_label = X.getRealLabel();

        // X <- alpha*(real-predicted)*X
        double val = learningFactor * (real_label - predicted_label);
        //System.out.println("Val: " + val);
        Map<Integer, Double> features = new TreeMap<Integer, Double>();
        for (Map.Entry<Integer, Double> e : X.getFeaturesMap().entrySet()) {
            features.put(e.getKey(), e.getValue() * val);
        }
        DataPoint adjusted_sample = new DataPoint(features, X.getRealLabel());
        //System.out.println("Data Point: " + adjusted_sample.getFeatures());
        // updating the model, no bias
        linearModel.addToW(adjusted_sample, 0.0);
    }

    /**
     * This function calculates the error value according to the exponential moving
     * average (i.e. error(i) =  alpha * X(i) + (1.0 - alpha)*error(i-1)
     * 			where X(i) = 1, if there was an error
     * 						 0, otherwise)
     *
     * @param wasThereError : A boolean variable specifying if there was an error or not
     * @throws Exception
     */
    private void update_exponentialMovingAverageError(boolean wasThereError) throws Exception {
        if (wasThereError) {
            expMovingAverage_error = expMovingAverage_a + (1.0 - expMovingAverage_a) * expMovingAverage_error;
        } else {
            expMovingAverage_error = (1.0 - expMovingAverage_a) * expMovingAverage_error;
        }
    }

    /**
     * This function performs (only) the prediction procedure taking into account the model,
     * of the given input sample
     *
     *  @param sample : the input DataPoint
     */
    @Override
    public double predict(DataPoint sample) throws Exception {
        return linearModel.predict(sample);
    }

    /**
     * This function is used for updating the exponential moving average of the
     * instances per class
     * @param X: the input of the model
     */
    @Override
    public void updateNum(DataPoint X) {
        // if this is the first time for the positive class initialise
        if (first_pos && X.getRealLabel() == 1) {
            M_pos = linearModel.score(X);
            first_pos = false;
            return;
        }
        // if this is the first time for the negative class initialise
        if (first_neg && X.getRealLabel() == -1) {
            M_neg = linearModel.score(X);
            first_neg = false;
            return;
        }
        // else
        if (X.getRealLabel() == 1) {
            M_pos = expMovingAverage_a * M_pos
                    + (1 - expMovingAverage_a) * linearModel.score(X);
        } else {
            M_neg = expMovingAverage_a * M_neg
                    + (1 - expMovingAverage_a) * linearModel.score(X);
        }
    }

    /**
     * This function saves the current model to a file
     *
     *  @param filename:The name of the file for the model to be stored
     * @throws IOException
     */
    @Override
    public void saveModel(String filename) throws IOException {

        filename += filename + ".model";

        linearModel.saveModel(filename);
    }

    public void saveLog(String filename) throws IOException {

        Results logResults = new Results(".", filename + ".log", true, true);

        String str = desiredPrecision + "\t"
                + threshold_for_precisionANDrecall + "\t"
                + change_of_thres + "\t"
                + learningFactor + "\t"
                + expMovingAverage_window + "\t"
                + expMovingAverage_error + "\t"
                + N_hat_Pos + "\t"
                + N_hat_Neg + "\t"
                + StatisticsMatrix[TP] + "\t"
                + StatisticsMatrix[FP] + "\t"
                + StatisticsMatrix[TN] + "\t"
                + StatisticsMatrix[FN] + "\t"
                + AUC_value + "\t"
                + N_overall;

        Date curDate = new Date();
        timestamp = curDate.getTime();
        logResults.print(Long.toString(this.timestamp) + "\t");
        logResults.println(str);
        logResults.SaveOutput();


    }

    /**
     * This function stores the header for the statistics to a file
     * (it is used only when the file does not already exists).
     *
     * @param filename:The filename the information of statistics will be stored
     */
    @Override
    public void writeHeader(String filename) throws IOException {
        // WRITE HEADER OF LOG FILE
        File loginfoFilename = new File(filename + ".log");

        BufferedWriter output;

        output = new BufferedWriter(new FileWriter(loginfoFilename));

        String header = "Timestamp \t desPrec \t extra_bias \t change_thres \t "
                + "eta \t EMAwin \t EMAerror \t N_pos \t N_neg \t "
                + "TP \t FP \t TN \t FN \t AUC \t N_overall \n";
        output.write(header);

        output.close();
        return;
    }

    /**
     * This function load the already stored model (if available) and the statistics
     * of the previous run (metaInfo).
     *
     * @param filename:The filename the model will be stored
     */
    @SuppressWarnings("resource")
    @Override
    public void loadModel(String filename) {
        try {
            // initialise the weights (if the model is not already saved make one with zeros)
            linearModel = new BasicLinearModel(filename + ".model");

            //LOAD LOG FILE
            File loginFilename = new File(filename + ".log");

            // counters for the number of positive or negative instances are met
            N_hat_Pos = 1.0;
            N_hat_Neg = 1.0;

            //No voc return.
            if (!loginFilename.exists()) {
                return;
            }


            BufferedReader input;

            input = new BufferedReader(new FileReader(loginFilename));

            //LOAD HEADER
            String line = null, tmp;

            while ((tmp = input.readLine()) != null) {
                if (!tmp.equals("")) {
                    line = tmp;
                }
            }
            //line = input.readLine();
            String toks[] = line.split("\t");
            if (toks[0].equalsIgnoreCase("Timestamp ")) {
                return;
            }
            timestamp = Long.parseLong(toks[0]);
            desiredPrecision = Double.parseDouble(toks[1]);
            threshold_for_precisionANDrecall = Double.parseDouble(toks[2]);
            change_of_thres = Double.parseDouble(toks[3]);
            learningFactor = Double.parseDouble(toks[4]);
            expMovingAverage_window = Integer.parseInt(toks[5]);
            expMovingAverage_error = Double.parseDouble(toks[6]);
            N_hat_Pos = Double.parseDouble(toks[7]);
            N_hat_Neg = Double.parseDouble(toks[8]);
            StatisticsMatrix[TP] = Long.parseLong(toks[9]);
            StatisticsMatrix[FP] = Long.parseLong(toks[10]);
            StatisticsMatrix[TN] = Long.parseLong(toks[11]);
            StatisticsMatrix[FN] = Long.parseLong(toks[12]);
            AUC_value = Double.parseDouble(toks[13]);
            setExpMovingAverage_a((2.0 / (expMovingAverage_window + 1.0)));
            N_overall = Integer.parseInt(toks[14]);

            input.close();
        } catch (Exception e1) {

            e1.printStackTrace();
        }
    }

    /**
     * Initialise matrix which holds statistics
     */
    @Override
    public void statsReset() {
        for (int i = 0; i < 4; i++) {
            StatisticsMatrix[i] = 0;
        }
    }

    /**
     * Returns the precision taking into account the confusion matrix
     */
    @Override
    public double statsGetPrecision() {
        if (StatisticsMatrix[TP] + StatisticsMatrix[FP] != 0) {
            double precision = (double) StatisticsMatrix[TP]
                    / (double) (StatisticsMatrix[TP] + StatisticsMatrix[FP]);
            return precision;
        } else {
            return 1.0;
        }
    }

    /**
     * Returns the value of the AUC of our experiment
     * @throws Exception
     */
    private void statsGetAUC(DataPoint sample) throws Exception {
        if (sample.getRealLabel() >= 0) {
            AUC_object.setLast_pos_score(linearModel.predict(sample));
        } else {
            AUC_object.setLast_neg_score(linearModel.predict(sample));
        }

        //System.out.println(AUC_object.getLast_pos_score()+ " , " +
        //	AUC_object.getLast_neg_score());

        AUC_object.expAUC_addPoint(AUC_object.getLast_pos_score()
                > AUC_object.getLast_neg_score());

        AUC_value = AUC_object.expMovAvGetAUC();
    }

    /**
     * Returns the recall taking into account the confusion matrix
     */
    @Override
    public double statsGetRecall() {
        if (StatisticsMatrix[TP] + StatisticsMatrix[FN] != 0) {
            return (double) StatisticsMatrix[TP]
                    / (double) (StatisticsMatrix[TP] + StatisticsMatrix[FN]);
        } else {
            return 1.0;
        }
    }

    /**
     * Returns the number of the negative instances taking into account the confusion matrix
     */
    @Override
    public long statsGetN() {
        long sum = 0;
        for (int i = 0; i < 4; i++) {
            sum += StatisticsMatrix[i];
        }
        return sum;
    }

    /**
     * Returns the true positive according to the confusion matrix
     */
    @Override
    public long statsGetTP() {
        return StatisticsMatrix[TP];
    }

    /**
     * Returns the false positive according to the confusion matrix
     */
    @Override
    public long statsGetFP() {
        return StatisticsMatrix[FP];
    }

    /**
     * Returns the false negative according to the confusion matrix
     */
    @Override
    public long statsGetFN() {
        return StatisticsMatrix[FN];
    }

    /**
     * Returns the true negative according to the confusion matrix
     */
    @Override
    public long statsGetTN() {
        return StatisticsMatrix[TN];
    }

    /**
     * This function update the threshold for the decision taking into account the desired
     * precision
     *
     * @param precision:The value of the current precision
     */
    private void updateThres(double precision) {
        if (precision < desiredPrecision) {
            threshold_for_precisionANDrecall += change_of_thres;
        } else {
            threshold_for_precisionANDrecall -= change_of_thres;
        }
    }

    /**
     * This function informs the counters for the positives or negatives instances
     * according to the exponential moving average
     * (i.e. counter(i) =  X(i) + (1.0 - alpha)*counter(i-1)
     * 			where X(i) = 1, if there was a positive input
     * 						 0, otherwise
     * 						- for the positive counter and the opposite for
     * 							the negative ones)
     *
     * @param whichClass : A variable specifying in which class the instance belongs
     * @throws Exception
     */
    private void update_exponentialMovingAverageCounter(int whichClass) throws Exception {
        if (whichClass == POSITIVE_LABEL) {
            //N_hat_Pos =  expMovingAverage_a + (1.0 - expMovingAverage_a)*N_hat_Pos;
            N_hat_Pos = 1.0 + (1.0 - expMovingAverage_a) * N_hat_Pos;

            N_hat_Neg = (1.0 - expMovingAverage_a) * N_hat_Neg;
        } else {
            N_hat_Pos = (1.0 - expMovingAverage_a) * N_hat_Pos;

            //N_hat_Neg =  expMovingAverage_a + (1.0 - expMovingAverage_a)*N_hat_Neg;
            N_hat_Neg = 1.0 + (1.0 - expMovingAverage_a) * N_hat_Neg;
        }
    }

    /**
     * This function sets the N (window)-number of instances to remember for the
     * calculation of the exponential moving average, and the alpha (the smoothing factor)
     * @param windowSize: the size of the memory window
     */
    @Override
    public void expMovAvSetWindow(int windowSize) {
        expMovingAverage_window = windowSize;
        setExpMovingAverage_a((2.0 / (expMovingAverage_window + 1.0)));

        AUC_object.expMovAvSetWindow(windowSize);
    }

    /**
     * This function outputs in the proper file the confusion matrix
     */
    @Override
    public void statsPrintConfusionMatrix() {
        String strTXT = "\t\tActual class\n"
                + "\t\tP\tN\n"
                + "Pred.P\t"
                + statsGetTP() + "\t" + statsGetFP() + " \n ";
        strTXT = strTXT + "Pred.N\t"
                + statsGetFN() + "\t" + statsGetTN() + " \n ";

        System.out.println(strTXT);
    }

    /**
     * Returns the threshold r used for the decision (<w*x + b> >= r)
     */
    @Override
    public double getDecisionThreshold() {
        return threshold_for_precisionANDrecall;
    }

    /**
     * Returns the current value of the exponential moving error average
     */
    @Override
    public double expMovAvGetError() {
        return expMovingAverage_error;
    }

    /**
     * This function sets a value for the alpha used in the calculation of the
     * exponential moving average (X(t) = alpha*Y(t) + (1-alpha)*X(t-1))
     * @param b: the value for the alpha to be set
     */
    private void setExpMovingAverage_a(double b) {
        expMovingAverage_a = b;
    }

    /**
     * This function sets a value for the desired precision we wish to have in the
     * classification method
     * @param presicion: the value for the desired precision to be set
     */
    @Override
    public void setDesiredPrecision(double precision) {
        desiredPrecision = precision;
    }

    /**
     * The first value of the exponential moving average (initialisation)
     */
    @Override
    public void expMovAvReset() throws Exception {
        // initial value for the error
        //expMovingAverage_error = 1.0;
        expMovingAverage_error = 0.0;
    }

    /**
     * Returns the current value of exponential moving average of the number of pos
     */
    @Override
    public double expMovAvGetPositives() {
        return N_hat_Pos;
    }

    /**
     * Returns the current value of exponential moving average of the number of neg
     */
    @Override
    public double expMovAvGetNegatives() {
        return N_hat_Neg;
    }

    /**
     * Returns the model used in the algorithm
     */
    @Override
    public LinearModel getLinearModel() {
        //TODO: This should return a deep copy!!!
        return linearModel;
    }

    /**
     * Returns the value of the desired precision
     */
    @Override
    public double getDesiredPrecision() {
        return desiredPrecision;
    }

    /**
     * Sets the decision threshold
     * (this function should not be used -for now- cause the thres is set automatically)
     */
    @Override
    public void setDecisionThreshold(double threshold) throws Exception {
        throw new Exception("Threshold is set automatically");
//		threshold_for_precisionANDrecall = threshold;
    }

    /**
     * Sets the learning factor (eta) with the value provided by its input
     * @param learningFactor: the value we wish to set the eta
     */
    @Override
    public void setLearningFactor(double learningFactor) {
        this.learningFactor = learningFactor;
    }

  

    /**
     * Returns the learning factor (eta) of the algorithm
     */
    @Override
    public double getLearningFactor() {
        return this.learningFactor;
    }

    /**
     * Returns true if the user wish to update the learning factors per class
     * and false if not
     */
    @Override
    public boolean getupdateLearningFactor() {
        return this.updateLearningFactor;
    }

    /**
     * Sets true if the user wish to update the learning factors per class
     * and false if not
     */
    public void setUpdateLearningFactor(boolean value) {
        this.updateLearningFactor = value;
    }

    /**
     * Returns the bias of the model (<wx+b>, where b is the bias)
     */
    @Override
    public double getBias() {
        return linearModel.getB();
    }

    /**
     * Returns the value of the AUC
     */
    @Override
    public double getAUC() {
        return AUC_value;
    }

    /**
     * Sets the number of the overall data that were classified
     * @param number
     */
    public void incrementN_overall(int addNumber) {
        N_overall += addNumber;
    }

    /**
     * @return the num_ok_p
     */
    public int getNum_ok_p() {
        return num_ok_p;
    }

    /**
     * @return the run_p
     */
    public int getRun_p() {
        return run_p;
    }

    /**
     * @param run_p the run_p to set
     */
    public void setRun_p(int run_p) {
        this.run_p = run_p;
    }

    /**
     * @param num_ok_p the num_ok_p to set
     */
    public void setNum_ok_p(int num_ok_p) {
        this.num_ok_p = num_ok_p;
    }

    public void saveWeightVector(TreeMap<Integer, Double> w, String fileName) {
        System.out.print("Writing Vocabulary...");


        StringBuilder s = new StringBuilder();

        s.append("Vocabulary Format: <id>\t<value>\n");
        Double value = null;
        Integer id = null;

        System.out.println("voc size : " + w.size());

        id = 0;
        value = w.get(0);

        s.append(id + "\t" + value + "\n");

        for (Map.Entry<Integer, Double> e : w.entrySet()) {
            try {
                id = e.getKey();
                value = e.getValue();
            } catch (Exception ex) {
                System.out.println("Error writing voc: " + ex.toString());
                continue;
            }

            if ((id != null) && (value != null) && (value != 0)) {
                s.append(id + "\t" + value + "\n");
            }
        }
        BufferedWriter fp;
        try {
            fp = new BufferedWriter(new FileWriter(fileName + ".model"));
            fp.write(s.toString());
            fp.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        System.out.println("DONE");



    }

    public long[] getStatisticsMatrix() {
        return StatisticsMatrix;
    }

    /**
     * @return the run_w
     */
    public int getRun_w() {
        return run_w;
    }

    /**
     * @param run_w the run_w to set
     */
    public void setRun_w(int run_w) {
        this.run_w = run_w;
    }

    /**
     * @return the num_ok_w
     */
    public int getNum_ok_w() {
        return num_ok_w;
    }

    /**
     * @param num_ok_w the num_ok_w to set
     */
    public void setNum_ok_w(int num_ok_w) {
        this.num_ok_w = num_ok_w;
    }

    public void setPosMargin(double margin) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNegMargin(double margin) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
