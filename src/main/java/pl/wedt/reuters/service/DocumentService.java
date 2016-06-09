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
    private Map<CategoryType, List<DocumentFiltered>> testDocuments, trainingDocuments;
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

        
        // tworzenie list z treningowymi i testowymi dokumentami, BEZ PODZIAŁU NA KATEGORIĘ
        createFilteredDocumentsLists(documentVectorList, featurePositionList);

        //te dokumenty nie są już potrzebne
        documentRawList = null;
         
    }

    /**
     * Tworzy listy testDocuments i trainingDocuments, zawierające dokumenty w formie do przetwarzania. 
     * @param documentVectorList 
     * @param featurePositionList
     */
	private void createFilteredDocumentsLists(double[][] documentVectorList, int featurePositionList[][]) {
		logger.info("Tworzenie przetworzonych dokumentów");

        testDocuments = new HashMap<CategoryType, List<DocumentFiltered>>();
        trainingDocuments = new HashMap<CategoryType, List<DocumentFiltered>>();
        Arrays.stream(CategoryType.values()).forEach(categoryType -> {
        	testDocuments.put(categoryType, new ArrayList<DocumentFiltered>());
        	trainingDocuments.put(categoryType, new ArrayList<DocumentFiltered>()); 
        });
        
        IntStream.range(0, documentVectorList.length).forEach(i -> {
            DocumentRaw documentRaw = documentRawList.get(i);
            
//            if (DocumentType.TEST.equals(documentRaw.getDocumentType())) {
//            	return; 
//            }
            
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
        
        // losowo tasujemy dokumenty - dzięki temu przy podziale na 10 wystarczy brać kolejne przedziały
        Arrays.stream(CategoryType.values()).forEach(categoryType -> Collections.shuffle(trainingDocuments.get(categoryType)));
	}

	/**
	 * Dodaje instancję DocumentFiltered o zadanych parametrach na listę testowych bądź treningowych dokumentów, w zależności od documentType.  
	 * @param documentType typ dokumentu (testowy, treningowy)
	 * @param categoryType kategoria dokumentu
	 * @param cat id kategorii dokumentu
	 * @param vector wektor dokumentu
	 * @param featurePositionList 
	 */
    private void addDocumentFiltered(DocumentType documentType, CategoryType categoryType, Integer cat, double[] vector, int[] featurePositionList ) {
    	switch (documentType) {
    	case TEST:
    			testDocuments.get(categoryType).add(new DocumentFiltered(cat, vector, featurePositionList)); 
    		break; 
    		
    	case TRAIN:
    			trainingDocuments.get(categoryType).add(new DocumentFiltered(cat, vector, featurePositionList));
    		break; 
    	}
    }
    
    /**
     * Pobiera listę dokumentów do trenowania w danym przebiegu cross-validacji. 
     * Razem z listą dokumentów do walidacji z tymi samymi parametrami sumują się do całej listy trainingDocuments.
     * TrainingDocuments jest ułożona losowo; więc jeśli weźmiemy kolejne przedziały to będzie jak dzielenie 
     * listy na 10 części. Ta funkcja zwraca "całą resztę" oprócz danego przedziału. 
     *  
     * @param runsNumber Liczba przebiegów (prawd. 10)
     * @param runId Numer tego przebiegu (liczony od 0)
     * @return lista dokumentów do trenowania w danym przebiegu
     */
	public List<DocumentFiltered> getDocumentToTrain(CategoryType categoryType, int runsNumber, int runId) {
		int idStart = getValidationListStartIndex(trainingDocuments.get(categoryType).size(), runsNumber, runId);
		int idEnd = getValidationListEndIndex(trainingDocuments.get(categoryType).size(), runsNumber, runId); 
		List<DocumentFiltered> ret = new ArrayList<DocumentFiltered>();  
		ret.addAll(trainingDocuments.get(categoryType).subList(0, idStart)); 
		List<DocumentFiltered> ret2 = trainingDocuments.get(categoryType).subList(idEnd, trainingDocuments.get(categoryType).size()); 
		ret.addAll(ret2);
		return ret; 
	}
	
	/**
     * Pobiera listę dokumentów do walidacji w danym przebiegu cross-validacji. 
     * Razem z listą dokumentów do trenowania z tymi samymi parametrami sumują się do całej listy trainingDocuments.
     * @param runsNumber Liczba przebiegów (prawd. 10)
     * @param runId Numer tego przebiegu (liczony od 0)
     * @return lista dokumentów do walidacji w danym przebiegu
     */
	public List<DocumentFiltered> getDocumentsToValidate(CategoryType categoryType, int runsNumber, int runId) {
		int idStart = getValidationListStartIndex(trainingDocuments.get(categoryType).size(), runsNumber, runId);
		int idEnd = getValidationListEndIndex(trainingDocuments.get(categoryType).size(), runsNumber, runId);
		return trainingDocuments.get(categoryType).subList(idStart, idEnd); 
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


	public List<DocumentFiltered> getTestDocuments(CategoryType categoryType) {
		return testDocuments.get(categoryType);
	}


	public List<DocumentFiltered> getTrainingDocuments(CategoryType categoryType) {
		return trainingDocuments.get(categoryType);
	}

}
