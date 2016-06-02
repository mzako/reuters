package pl.wedt.reuters.application;

import org.apache.log4j.Logger;
import pl.wedt.reuters.classifier.SVM;
import pl.wedt.reuters.model.CategoryType;
import pl.wedt.reuters.service.DocumentService;
import pl.wedt.reuters.utils.Category;
import pl.wedt.reuters.utils.PropertiesLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;


/**
 * @author Michał Żakowski
 *
 * Klasa startowa aplikacji
 */

public class Main {
    private final static Logger logger = Logger.getLogger(Main.class);
    private final static Integer DOCUMENT_NUMBER = 2;

    public static void main(String [] args) {
        String resourcesPath = Main.class.getClassLoader().getResource("reuters21578").getPath();
        long startTime = System.nanoTime();

        try {
            Properties properties =
                    new PropertiesLoader(Main.class.getClassLoader().getResource("app.properties").getPath())
                        .getProperties();
            Category.loadData(resourcesPath);

            List<String> documentNames = new ArrayList<>();
            IntStream.range(0, DOCUMENT_NUMBER).forEach(i -> {
                documentNames.add("reut2-" + String.format("%03d", i) + ".sgm");
            });

            DocumentService documentService = new DocumentService(resourcesPath, documentNames, "stop_list.txt");
            logger.info("Ładowanie dokumentów");
            documentService.loadDocuments();
            logger.info("Przetwarzanie dokumentów");
            documentService.prepareDocuments();
            logger.info("Klasyfikacja SVM");
            SVM svm = new SVM(documentService, Double.valueOf(properties.getProperty("svm.C")),
                    Double.valueOf(properties.getProperty("svm.eps")));
            svm.classify2(CategoryType.PLACES, Arrays.asList(161, 158));
        } catch(Exception e) {
            e.printStackTrace();
        }

        logger.info("Czas działania: " + (System.nanoTime() - startTime)/1000000000.0);
    }
}
