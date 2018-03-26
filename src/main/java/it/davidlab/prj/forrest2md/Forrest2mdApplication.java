package it.davidlab.prj.forrest2md;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.*;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootApplication
public class Forrest2mdApplication {

    int sectionLevel = 1;
    boolean insideA = false;
    String link = "";

    public static void main(String[] args) {
        SpringApplication.run(Forrest2mdApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext context) {
        return args -> {

            String xmlPath = args[0];
            String xmlFileName = args[1];

            StringBuilder mdString = new StringBuilder();
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader eventReader =
                    factory.createXMLEventReader(new FileReader("/Users/david/Desktop/concepts.xml"));

            eventReader.forEachRemaining(e -> {
                XMLEvent event = (XMLEvent) e;

                switch (event.getEventType()) {

                    case XMLStreamConstants.START_ELEMENT: {
                        StartElement startElement = event.asStartElement();
                        String qName = startElement.getName().getLocalPart().toLowerCase();
                        switch (qName) {
                            case "section":
                                sectionLevel++;
                                break;

                            case "title":
                                mdString.append("\n");
                                IntStream.range(0, sectionLevel).forEach(i -> mdString.append('#'));
                                mdString.append(" ");
                                break;

                            case "p":
                                break;

                            case "em":
                                mdString.append("_");
                                break;

                            case "ul":
                                break;

                            case "li":
                                mdString.append("* ");
                                break;

                            case "strong":
                                mdString.append("**");
                                break;

                            case "br":
                                mdString.append("\n");
                                break;

                            case "figure":
                                String src = startElement.getAttributeByName(new QName("src")).getValue();
                                String alt = startElement.getAttributeByName(new QName("alt")).getValue();

                                mdString.append("![").append(alt).append("](").append(src).append(")");
                                break;

                            case "a":
                                insideA = true;
                                link = startElement.getAttributeByName(new QName("href")).getValue();
                                mdString.append("[");
                                break;

                            case "code":
                                mdString.append("`");
                                break;
                        }
                    }
                    break;

                    case XMLStreamConstants.CHARACTERS:
                        Characters characters = event.asCharacters();
//                        String text = characters.getData().trim();
                        String text = characters.getData();
                        String cleanedText = text
                                .replaceAll("[ \\t]{2,}", " ")
                                .replaceAll("[\\n]+", "");
                        if (!cleanedText.trim().isEmpty()) {
                            mdString.append(cleanedText);
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        EndElement endElement = event.asEndElement();

                        String qName = endElement.getName().getLocalPart().toLowerCase();

                        switch (qName) {

                            case "section":
                                sectionLevel--;
                                break;

                            case "title":
                                mdString.append("\n");
                                break;

                            case "p":
                                mdString.append("\n");
                                break;

                            case "em":
                                mdString.append("_");
                                break;

                            case "ul":
                                break;

                            case "li":
                                mdString.append("\n");
                                break;

                            case "strong":
                                mdString.append("**");
                                break;

                            case "a":
                                insideA = false;
                                mdString.append("]").append("(").append(link).append(")");

                            case "code":
                                mdString.append("`");
                                break;

                        }
                        break;


                }


            });

            Path path = Paths.get(xmlFileName + ".md");

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(mdString.toString());
            }

        };
    }


}//End class


