package pl.wedt.reuters.parser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import pl.wedt.reuters.model.DocumentRaw;
import pl.wedt.reuters.model.DocumentType;
import pl.wedt.reuters.utils.Category;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Michał Żakowski
 *
 * Klasa parsera dokumentów
 */
public class Parser {
    //ładuje listę dokumentów w postaci nieprzetworzonej
    public List<DocumentRaw> loadDocumentFile(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        lines.remove(0);
        String file = lines.stream().collect(Collectors.joining("\n"));

        //usuwanie odnośników znakowych
        file = "<DOC>" + file.replaceAll("&#[0-9]*;", "") + "</DOC>";
        Document document = parseSGML(new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)));
        NodeList nodeList = document.getElementsByTagName("REUTERS");

        List<DocumentRaw> documentRawList = new ArrayList<>();
        //parsowanie dokumentów do postaci nieprzetworzonej
        IntStream.range(0, nodeList.getLength()).forEach(i -> {
            DocumentRaw documentRaw = parseDocument((Element) nodeList.item(i));
            if(documentRaw != null) {
                documentRawList.add(documentRaw);
            }
        });

        return documentRawList;
    }

    //parsowanie struktury SGML
    private Document parseSGML(InputStream file) {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document = docBuilder.parse(file);
            document.getDocumentElement().normalize();

            return document;
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    //parsowanie węzła drzewa xml reprezentującego dokument
    private DocumentRaw parseDocument(Element document) {
        String topicsAttr = document.getAttributes().getNamedItem("TOPICS").getTextContent();
        String lewissplitAttr = document.getAttributes().getNamedItem("LEWISSPLIT").getTextContent();
        DocumentType documentType;

        //Podział na część trenującą i uczącą wg ModApte Split
        if(topicsAttr.equals("YES") && lewissplitAttr.equals("TRAIN")) {
            documentType = DocumentType.TRAIN;
        }
        else if(topicsAttr.equals("YES") && lewissplitAttr.equals("TEST")) {
            documentType = DocumentType.TEST;
        }
        else {
            //dokument niewykorzystywany
            return null;
        }

        //parsowanie kategorii w podziale na typy kategorii
        List<String> exchangesStrings = parseCategories(document, "EXCHANGES");
        List<Integer> exchangesIds = exchangesStrings.stream()
                .map(i -> Category.EXCHANGES.get(i)).collect(Collectors.toList());

        List<String> orgsStrings = parseCategories(document, "ORGS");
        List<Integer> orgsIds = orgsStrings.stream()
                .map(i -> Category.ORGS.get(i)).collect(Collectors.toList());

        List<String> peopleStrings = parseCategories(document, "PEOPLE");
        List<Integer> peopleIds = peopleStrings.stream()
                .map(i -> Category.PEOPLE.get(i)).collect(Collectors.toList());

        List<String> placesStrings = parseCategories(document, "PLACES");
        List<Integer> placesIds = placesStrings.stream()
                .map(i -> Category.PLACES.get(i)).collect(Collectors.toList());

        List<String> topicsStrings = parseCategories(document, "TOPICS");
        List<Integer> topicsIds = topicsStrings.stream()
                .map(i -> Category.TOPICS.get(i)).collect(Collectors.toList());

        //właściwa treść dokumentu
        Node bodyNode = ((Element) document.getElementsByTagName("TEXT").item(0)).getElementsByTagName("BODY").item(0);

        if(bodyNode == null) {
            return null;
        }

        String body = bodyNode.getTextContent();
        DocumentRaw documentRaw = new DocumentRaw(body, documentType, exchangesIds, orgsIds, peopleIds,
                placesIds, topicsIds);

        return documentRaw;
    }

    //parsuje dany typ kategrii
    private List<String> parseCategories(Element document, String category) {
        NodeList nodeList = document.getElementsByTagName(category).item(0).getChildNodes();
        List<String> categories = new ArrayList<>();
        IntStream.range(0, nodeList.getLength()).forEach(i -> categories.add(nodeList.item(i).getTextContent()));

        return categories;
    }
}
