package pl.wedt.reuters.application;

import org.apache.log4j.Logger;
import pl.wedt.reuters.service.DocumentService;
import pl.wedt.reuters.utils.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


/**
 * @author Michał Żakowski
 *
 * Klasa startowa aplikacji
 */

public class Main {
    private final static Logger logger = Logger.getLogger(Main.class);
    private final static Integer DOCUMENT_NUMBER = 22;

    public static void main(String [] args) {
        String resourcesPath = Main.class.getClassLoader().getResource("reuters21578").getPath();
        try {
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
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
