package pl.wedt.reuters.classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;

import com.google.common.collect.Maps;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import pl.wedt.reuters.model.DocumentFiltered;
import pl.wedt.reuters.service.DocumentService;

/**
 * @author Michał Żakowski
 */
public class SVM {
    private final static Logger logger = Logger.getLogger(SVM.class);
    private Model model;
    private double C;
    private double eps;
    private int dim; 
    private SolverType solver;

    public SVM(double C, double eps, int dim, SolverType solver) {
        this.C = C;
        this.eps = eps;
        this.dim = dim; 
        this.solver = solver;
    }

    //One versus All SVM
    public List<Integer> clsOVA(final List<DocumentFiltered> trainSet, final List<DocumentFiltered> testSet) {
        int trainSize = trainSet.size();
        FeatureNode trainNodes[][] = new FeatureNode[trainSize][];
        double labels[] = new double[trainSize];
        List<Model> models = new ArrayList<>();

        List<Integer> categories = trainSet.stream().map(d -> d.getCategory()).distinct().collect(Collectors.toList());

        categories.stream().forEach(c -> {
            IntStream.range(0, trainSize).forEach(i -> {
                int cat = trainSet.get(i).getCategory();
                if (cat == c) {
                    labels[i] = 0;
                } else {
                    labels[i] = 1;
                }

                double dvec[] = trainSet.get(i).getVector();
                int fvec[] = trainSet.get(i).getFeaturePosition();
                trainNodes[i] = new FeatureNode[dvec.length];
                for (int j = 0; j < dvec.length; j++) {
                    trainNodes[i][j] = new FeatureNode(fvec[j] + 1, dvec[j]);
                }
                ;
            });

            logger.info("Trenowanie klasy: " + c);
            models.add(train(trainSize, dim, trainNodes, labels, C, eps, solver));
        });

        int testSize = testSet.size();
        double labCls[][] = new double[models.size()][testSize];
        FeatureNode testNodes[][] = new FeatureNode[testSize][];
        IntStream.range(0, testSize).forEach(i -> {
            double dvec[] = testSet.get(i).getVector();
            int fvec[] = testSet.get(i).getFeaturePosition();

            testNodes[i] = new FeatureNode[dvec.length];
            for (int j = 0; j < dvec.length; j++) {
                testNodes[i][j] = new FeatureNode(fvec[j] + 1, dvec[j]);
            }
            ;
        });

        IntStream.range(0, models.size()).forEach(i -> {
            IntStream.range(0, testSize).forEach(j -> {
                labCls[i][j] = Linear.predict(models.get(i), testNodes[j]);
            });
        });

        List<Integer> finalCls = new ArrayList<>();

        for (int j = 0; j < testSize; j++) {
            int positive = 0;
            for (int i = 0; i < models.size(); i++) {
                if (labCls[i][j] == 0) {
                    positive++;
                }
            }
            if (positive == 1) {
                final int k = j;
                finalCls.add(categories.get(IntStream.range(0, models.size())
                        .filter(p -> labCls[p][k] == 0).findFirst().getAsInt()));
            } else {
                finalCls.add(-1);
            }
        }

        return finalCls;
    }

    class OVOModel {
        int i;
        int j;
        Model model;

        public OVOModel(int i, int j, Model model) {
            this.i = i;
            this.j = j;
            this.model = model;
        }

        public int getI() {
            return i;
        }

        public int getJ() {
            return j;
        }

        public Model getModel() {
            return model;
        }
    }

    //One versus One SVM
    public List<Integer> clsOVO(final List<DocumentFiltered> trainSet, final List<DocumentFiltered> testSet) {
        int trainSize = trainSet.size();
        FeatureNode trainNodes[][];
        double labels[] = new double[trainSize];

        List<Integer> categories = trainSet.stream().map(d -> d.getCategory()).distinct().collect(Collectors.toList());
        Map<Integer, Integer> categoryMap = Maps.newHashMap();
        IntStream.range(0, categories.size()).forEach(i -> {
            if (categoryMap.get(categories.get(i)) == null) {
                categoryMap.put(categories.get(i), i);
            }
        });
        List<OVOModel> ovoModels = new ArrayList<>();

        for (int i = 0; i < categories.size() - 1; i++) {
            for (int j = i + 1; j < categories.size(); j++) {
                final int ii = i;
                final int jj = j;
                List<DocumentFiltered> docs = trainSet.stream()
                        .filter(d -> d.getCategory().equals(categories.get(ii))
                                || d.getCategory().equals(categories.get(jj)))
                        .collect(Collectors.toList());

                trainNodes = new FeatureNode[docs.size()][];
                for (int k = 0; k < docs.size(); k++) {
                    double dvec[] = docs.get(k).getVector();
                    int fvec[] = docs.get(k).getFeaturePosition();

                    trainNodes[k] = new FeatureNode[dvec.length];
                    for (int l = 0; l < dvec.length; l++) {
                        trainNodes[k][l] = new FeatureNode(fvec[l] + 1, dvec[l]);
                    }

                    labels[k] = docs.get(k).getCategory();

                }

                logger.info("Trenowanie klas: " + i + " i " + j);
                Model model = train(docs.size(), dim, trainNodes, labels, C, eps, solver);
                ovoModels.add(new OVOModel(i, j, model));
            }
        }

        int testSize = testSet.size();
        FeatureNode testNodes[][] = new FeatureNode[testSize][];
        List<Integer> finalCls = new ArrayList<>();

        int votingVec[] = new int[categories.size()];

        for (int i = 0; i < testSize; i++) {
            for (int j = 0; j < ovoModels.size(); j++) {
                double dvec[] = testSet.get(i).getVector();
                int fvec[] = testSet.get(i).getFeaturePosition();

                testNodes[i] = new FeatureNode[dvec.length];
                for (int k = 0; k < dvec.length; k++) {
                    testNodes[i][k] = new FeatureNode(fvec[k] + 1, dvec[k]);
                }
                double label = Linear.predict(ovoModels.get(j).getModel(), testNodes[i]);
                votingVec[categoryMap.get((int) label)] += 1;
            }

            int nrMax = 0;
            int maxVal = 0;
            int maxId = 0;
            for (int j = 0; j < votingVec.length; j++) {
                if (votingVec[j] > maxVal) {
                    nrMax = 1;
                    maxVal = votingVec[j];
                    maxId = j;
                } else if (votingVec[j] == maxVal) {
                    nrMax++;
                }
            }

            if (nrMax == 0 || nrMax > 1) {
                finalCls.add(-1);
            } else {
                finalCls.add(categories.get(maxId));
            }

            for (int j = 0; j < votingVec.length; j++) {
                votingVec[j] = 0;
            }
        }

        return finalCls;
    }

    private Model train(int size, int features, Feature[][] nodes, double[] labels, double C, double eps, SolverType solver) {
        Problem problem = new Problem();
        problem.l = size;
        problem.n = features;
        problem.x = nodes;
        problem.y = labels;
        Parameter parameter = new Parameter(solver, C, eps);

        logger.info("Rozpoczynanie trenowania");
        return Linear.train(problem, parameter);
    }
}
