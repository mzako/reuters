package pl.wedt.reuters.classifier;

import de.bwaldvogel.liblinear.*;
import org.apache.log4j.Logger;
import pl.wedt.reuters.model.CategoryType;
import pl.wedt.reuters.model.DocumentFiltered;
import pl.wedt.reuters.service.DocumentService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Michał Żakowski
 */
public class SVM {
    private final static Logger logger = Logger.getLogger(SVM.class);
    private DocumentService documentService;
    private Model model;
    private double C;
    private double eps;

    public SVM(DocumentService documentService, double C, double eps) {
        this.documentService = documentService;
        this.C = C;
        this.eps = eps;
    }

    public void classify2(CategoryType categoryType, List<Integer> categories) {
        List<DocumentFiltered> documents = documentService.getDocumentFilteredMap()
                .get(categoryType);

        List<DocumentFiltered> documentsFromCategories = new ArrayList<>();
        categories.stream().forEach(c -> {
            documentsFromCategories.addAll(documents
                    .stream().filter(d -> d.getCategory().equals(c)).collect(Collectors.toList()));
        });

        int totalSize = documentsFromCategories.size();
        int dim = documentsFromCategories.get(0).getVector().length;

        FeatureNode nodes[][] = new FeatureNode[totalSize][dim];
        double labels[] = new double[totalSize];

        IntStream.range(0, totalSize).forEach(i -> {
            IntStream.range(0, dim).forEach(j -> {
                double df[] = documentsFromCategories.get(i).getVector();
                nodes[i][j] = new FeatureNode(j + 1, df[j]);
            });
            labels[i] = documentsFromCategories.get(i).getCategory();
        });

        model = train(totalSize, dim, nodes, labels, C, eps);

        int errors = 0;
        for(int i = 0; i < totalSize; i++) {
            if(Linear.predict(model, nodes[i]) != labels[i]) {
                errors++;
            }
        }
        logger.info("Jakość: " + (1.0 - (double) errors / totalSize));
    }

    private Model train(int size, int features, Feature[][] nodes, double[] labels, double C, double eps) {
        Problem problem = new Problem();
        problem.l = size;
        problem.n = features;
        problem.x = nodes;
        problem.y = labels;

        SolverType solver = SolverType.L1R_LR;

        Parameter parameter = new Parameter(solver, C, eps);

        logger.info("Rozpoczynanie trenowania");

        return Linear.train(problem, parameter);
    }

    private double predict(Feature[] vector) {
        return Linear.predict(model, vector);
    }
}
