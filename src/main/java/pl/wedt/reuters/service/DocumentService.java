package pl.wedt.reuters.service;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.log4j.Logger;
import pl.wedt.reuters.model.CategoryType;
import pl.wedt.reuters.model.DocumentFiltered;
import pl.wedt.reuters.model.DocumentRaw;
import pl.wedt.reuters.model.DocumentType;
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
    private List<DocumentFiltered> testDocuments, trainingDocuments;
    private int dim;

	public DocumentService(String resourcesPath, List<String> documentFileNames, String stopListFileName) {
    	if (resourcesPath != null && resourcesPath.startsWith("/")) 
    		resourcesPath = resourcesPath.substring(1); 
        this.resourcesPath = resourcesPath;
        this.documentFileNames = documentFileNames;
        this.parser = new Parser();
        this.tokenizer = new Porter();
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

        
        // tworzenie list z treningowymi i testowymi dokumentami, z podziałem na kategorię
        createFilteredDocumentsLists(documentVectorList, featurePositionList);

        //te dokumenty nie są już potrzebne
        documentRawList = null;
         
    }

	private void createFilteredDocumentsLists(double[][] documentVectorList, int featurePositionList[][]) {
		logger.info("Tworzenie przetworzonych dokumentów");

        testDocuments = new ArrayList<DocumentFiltered>();
        trainingDocuments = new ArrayList<DocumentFiltered>();
        
        IntStream.range(0, documentVectorList.length).forEach(i -> {
            DocumentRaw documentRaw = documentRawList.get(i);
            
            documentRaw.getExchanges().stream().forEach(cat -> {
            	addDocumentFiltered(documentRaw.getDocumentType(), CategoryType.EXCHANGES, cat, documentVectorList[i], featurePositionList[i]);
            });
            documentRaw.getOrgs().stream().forEach(cat -> {
            	addDocumentFiltered(documentRaw.getDocumentType(), CategoryType.ORGS, cat, documentVectorList[i], featurePositionList[i]);
            });
            documentRaw.getPeople().stream().forEach(cat -> {
            	addDocumentFiltered(documentRaw.getDocumentType(), CategoryType.PEOPLE, cat, documentVectorList[i], featurePositionList[i]);
            });
            documentRaw.getPlaces().stream().forEach(cat -> {
            	addDocumentFiltered(documentRaw.getDocumentType(), CategoryType.PLACES, cat, documentVectorList[i], featurePositionList[i]);
            });
            documentRaw.getTopics().stream().forEach(cat -> {
            	addDocumentFiltered(documentRaw.getDocumentType(), CategoryType.TOPICS, cat, documentVectorList[i], featurePositionList[i]);
            });
        });
        
        Collections.shuffle(trainingDocuments);
	}

    private void addDocumentFiltered(DocumentType documentType, CategoryType categoryType, Integer cat, double[] vector, int[] featurePositionList ) {
    	switch (documentType) {
    	case TEST:
    			testDocuments.add(new DocumentFiltered(categoryType, cat, vector, featurePositionList)); 
    		break; 
    		
    	case TRAIN:
    			trainingDocuments.add(new DocumentFiltered(categoryType, cat, vector, featurePositionList));
    		break; 
    	}
    }
    

	public List<DocumentFiltered> getDocumentToTrain(int howManyParts, int partNum) {
		int idStart = getValidationListStartIndex(trainingDocuments.size(), howManyParts, partNum);
		int idEnd = getValidationListEndIndex(trainingDocuments.size(), howManyParts, partNum); 
		List<DocumentFiltered> ret = new ArrayList<DocumentFiltered>();  
		ret.addAll(trainingDocuments.subList(0, idStart)); 
		List<DocumentFiltered> ret2 = trainingDocuments.subList(idEnd, trainingDocuments.size()); 
		ret.addAll(ret2);
		return ret; 
	}
	
	public List<DocumentFiltered> getDocumentsToValidate(int howManyParts, int partNum) {
		int idStart = getValidationListStartIndex(trainingDocuments.size(), howManyParts, partNum);
		int idEnd = getValidationListEndIndex(trainingDocuments.size(), howManyParts, partNum);
		return trainingDocuments.subList(idStart, idEnd); 
	}

	private int getValidationListStartIndex(int listSize, int howManyParts, int partNum) {
		int mod = listSize % howManyParts; 
		int div = listSize / howManyParts;
		return (partNum < mod ? (div+1)*partNum : (div+1)*mod + (partNum-mod)*div); 
	}
	
	private int getValidationListEndIndex(int listSize, int howManyParts, int partNum) {
		int mod = listSize % howManyParts; 
		int div = listSize / howManyParts;
		return (partNum < mod ? (div+1)*(partNum+1) : (div+1)*mod + (partNum-mod+1)*div); 
	}

	
    public int getDim() {
        return dim;
    }


	public List<DocumentFiltered> getTestDocuments() {
		return testDocuments;
	}


	public List<DocumentFiltered> getTrainingDocuments() {
		return trainingDocuments;
	}

}
