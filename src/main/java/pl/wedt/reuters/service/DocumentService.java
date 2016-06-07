package pl.wedt.reuters.service;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.log4j.Logger;
import pl.wedt.reuters.model.CategoryType;
import pl.wedt.reuters.model.DocumentFiltered;
import pl.wedt.reuters.model.DocumentRaw;
import pl.wedt.reuters.parser.Parser;
import pl.wedt.reuters.parser.Porter;
import pl.wedt.reuters.parser.StopListFilter;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Michał Żakowski
 *
 * Klasa do zarządzania dokumentami
 */
public class DocumentService {
    private final Logger logger = Logger.getLogger(DocumentService.class);

    private Parser parser;
    private Porter tokenizer;
    private StopListFilter stopListFilter;
    private String resourcesPath;
    private List<String> documentFileNames;
    private List<String> dictionaryVector;
    private List<DocumentRaw> documentRawList;
    private Map<CategoryType, List<DocumentFiltered>> documentFilteredMap;
    private int dim;

    public Map<CategoryType, List<DocumentFiltered>> getDocumentFilteredMap() {
        return documentFilteredMap;
    }

    public DocumentService(String resourcesPath, List<String> documentFileNames, String stopListFileName) {
        this.resourcesPath = resourcesPath;
        this.documentFileNames = documentFileNames;
        this.parser = new Parser();
        this.tokenizer = new Porter();
        this.stopListFilter = new StopListFilter(Paths.get(resourcesPath, stopListFileName));
    }

    public int getDim() {
        return dim;
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
            List<String> tokens = tokenizer.compute(i.getBody());
            //filtrowanie wg stop listy
            tokens = stopListFilter.filter(tokens);
            //zamiana na małe litery
            tokens = tokens.stream().map(t -> t.toLowerCase()).collect(Collectors.toList());
            //tworzenie bag of words dokumentu
            Multiset bagOfWords = HashMultiset.create();
            tokens.stream().forEach(token -> bagOfWords.add(token));
            documentBodyBagOfWordsList.add(bagOfWords);
            //dodanie słów z dokumentu do słownika
            dictionary.addAll(tokens);
        });

        //tworzenie wektora słownikowego, każda pozycja odpowiada jednemu słowu
        dictionaryVector = new ArrayList<>();
        dictionaryVector.addAll(dictionary);
        dim = dictionaryVector.size();
        logger.info("Wielkość słownika - " + dim);

        //tworzenie oddzielnych list dokumentów dla każdego typu kategorii
        documentFilteredMap = new HashMap<>();
        Arrays.stream(CategoryType.values()).forEach(cat -> documentFilteredMap.put(cat, new ArrayList<>()));

        logger.info("Normalizacja wektorów częstości");
        //tworzenie wektorów częstości
        double documentVectorList[][] = new double[documentBodyBagOfWordsList.size()][];
        int featurePositionList[][] = new int[documentBodyBagOfWordsList.size()][];

        IntStream.range(0, documentVectorList.length).forEach(i -> {
            //tworzenie wektora częstości słów dla dokumentu
            Multiset<String> bagOfWords = documentBodyBagOfWordsList.get(i);
            int nonZeros = (int)dictionaryVector.stream().filter(w -> bagOfWords.count(w) != 0).count();
            documentVectorList[i] = new double[nonZeros];
            featurePositionList[i] = new int[nonZeros];
            int pos = 0;
            for(int j = 0; j < dictionaryVector.size(); j++) {
                int count = bagOfWords.count(dictionaryVector.get(j));
                if(count != 0) {
                    featurePositionList[i][pos] = j;
                    documentVectorList[i][pos++] = count;
                }
            }
        });

        logger.info("Tworzenie przetworzonych dokumentów");

        IntStream.range(0, documentVectorList.length).forEach(i -> {
            DocumentRaw documentRaw = documentRawList.get(i);

            //tworzenie dokumentów dla każdego typu kategorii i dla różnych kategorii wg typu
            documentRaw.getExchanges().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.EXCHANGES).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.EXCHANGES, cat, documentVectorList[i], featurePositionList[i]));
            });
            documentRaw.getOrgs().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.ORGS).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.ORGS, cat, documentVectorList[i], featurePositionList[i]));
            });
            documentRaw.getPeople().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.PEOPLE).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.PEOPLE, cat, documentVectorList[i], featurePositionList[i]));
            });
            documentRaw.getPlaces().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.PLACES).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.PLACES, cat, documentVectorList[i], featurePositionList[i]));
            });
            documentRaw.getTopics().stream().forEach(cat -> {
                documentFilteredMap.get(CategoryType.TOPICS).add(new DocumentFiltered(documentRaw.getDocumentType(),
                        CategoryType.TOPICS, cat, documentVectorList[i], featurePositionList[i]));
            });
        });

        //te dokumenty nie są już potrzebne
        documentRawList = null;
    }
}
