package pl.wedt.reuters.service;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.log4j.Logger;
import pl.wedt.reuters.model.CategoryType;
import pl.wedt.reuters.model.DocumentFiltered;
import pl.wedt.reuters.model.DocumentRaw;
import pl.wedt.reuters.parser.Parser;
import pl.wedt.reuters.parser.StopListFilter;
import pl.wedt.reuters.parser.Tokenizer;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

/**
 * @author Michał Żakowski
 *
 * Klasa do zarządzania dokumentami
 */
public class DocumentService {
    private final Logger logger = Logger.getLogger(DocumentService.class);

    private Parser parser;
    private Tokenizer tokenizer;
    private StopListFilter stopListFilter;
    private String resourcesPath;
    private List<String> documentFileNames;
    List<String> dictionaryVector;
    List<DocumentRaw> documentRawList;
    Map<CategoryType, List<DocumentFiltered>> documentFilteredMap;

    public DocumentService(String resourcesPath, List<String> documentFileNames, String stopListFileName) {
        this.resourcesPath = resourcesPath;
        this.documentFileNames = documentFileNames;
        this.parser = new Parser();
        this.tokenizer = new Tokenizer();
        this.stopListFilter = new StopListFilter(Paths.get(resourcesPath, stopListFileName));
    }

    //wczytuje dokumenty w postaci nieprzetworzonej
    public void loadDocuments() {
        documentRawList = new ArrayList<>();
        documentFileNames.stream().forEach(i -> {
            documentRawList.addAll(parser.loadDocumentFile(Paths.get(resourcesPath, i)));
        });
    }

    //przetwarzanie wstępne dokumentów
    public void prepareDocuments() {
        Set<String> dictionary = new TreeSet<>();

        List<Multiset<String>> documentBodyBagOfWordsList = new ArrayList<>();
        logger.info("Tworzenie słownika");
        documentRawList.stream().forEach(i -> {
            //rozbij na słowa i zamień na lemmy
            List<String> tokens = tokenizer.tokenize(i.getBody());
            //filtrowanie wg stop listy
            tokens = stopListFilter.filter(tokens);
            //tworzenie bag of words dokumentu
            Multiset bagOfWords = HashMultiset.create();
            tokens.stream().forEach(token -> bagOfWords.add(token));
            documentBodyBagOfWordsList.add(bagOfWords);
            //dodanie słów z dokumentu do słownika
            dictionary.addAll(tokens);
        });

        //usuwanie ze słownika słów, które występują tylko w jednym dokumencie
        logger.info("Usuwanie zbędnych słów ze słownika");
        for (Iterator<String> iterator = dictionary.iterator(); iterator.hasNext();) {
            int nrDocs = 0;
            String word = iterator.next();
            for(Multiset<String> bag : documentBodyBagOfWordsList) {
                int count = bag.count(word);
                nrDocs = nrDocs + ((count > 0) ? count : 0);
            };
            if(nrDocs <= 1) {
                iterator.remove();
            }
        };

        //tworzenie wektora słownikowego, każda pozycja odpowiada jednemu słowu
        dictionaryVector = new ArrayList<>();
        dictionaryVector.addAll(dictionary);

        //tworzenie oddzielnych list dokumentów dla każdego typu kategorii
        documentFilteredMap = new HashMap<>();
        Arrays.stream(CategoryType.values()).forEach(cat -> documentFilteredMap.put(cat, new ArrayList<>()));

        logger.info("Tworzenie przetworzonych dokumentów");
        IntStream.range(0, documentBodyBagOfWordsList.size()).forEach(i -> {
            List<Integer> documentVector = new ArrayList<>();

            //tworzenie wektora częstości słów dla dokumentu
            dictionaryVector.stream().forEach(word ->
                documentVector.add(documentBodyBagOfWordsList.get(i).count(word))
            );
            DocumentRaw documentRaw = documentRawList.get(i);

            //tworzenie dokumentów dla każdego typu kategorii i dla różnych kategorii wg typu
            documentRaw.getExchanges().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.EXCHANGES).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.EXCHANGES, cat, documentVector));
            });
            documentRaw.getOrgs().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.ORGS).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.ORGS, cat, documentVector));
            });
            documentRaw.getPeople().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.PEOPLE).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.PEOPLE, cat, documentVector));
            });
            documentRaw.getPlaces().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.PLACES).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.PLACES, cat, documentVector));
            });
            documentRaw.getTopics().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.TOPICS).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.TOPICS, cat, documentVector));
            });
        });

        //te dokumenty nie są już potrzebne
        documentRawList = null;
    }
}
