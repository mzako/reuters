package pl.wedt.reuters.parser;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Michał Żakowski
 *
 * Klasa do zamiany tekstu na słowa
 */
public class Tokenizer {
    public List<String> tokenize(String text) {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        Annotation document = new StanfordCoreNLP(props, false).process(text);

        List<String> tokens = new ArrayList<>();

        for(CoreMap sentence: document.get(CoreAnnotations.SentencesAnnotation.class))
        {
            for(CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class))
            {
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                tokens.add(lemma);
            }
        }

        //usuwanie słów o dł. równej 1 będących znakami innymi niż litery oraz słów zawierających liczby
        tokens = tokens.stream().filter(i -> !((i.length() > 1 && i.matches(".*\\d+.*")) ||
                (i.length() == 1 && !Character.isLetter(i.charAt(0)))))
                .collect(Collectors.toList());

        return tokens;
    }
}
