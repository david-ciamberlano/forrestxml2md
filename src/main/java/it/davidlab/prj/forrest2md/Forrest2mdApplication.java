package it.davidlab.prj.forrest2md;

import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

@SpringBootApplication
public class Forrest2mdApplication {

    Logger logger = LoggerFactory.getLogger(Forrest2mdApplication.class);

    int sectionLevel = 1;
    boolean insideA = false;
    String link = "";

    public static void main(String[] args) {
        SpringApplication.run(Forrest2mdApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext context) {
        return args -> {

            if (args.length != 2) {
                logger.error("usage: <forrest2md>  \"/path/to/source.xml\" \"/path/to/dest.md\"");
                return;
            }

            String xmlPath = args[0];
            String mdDest = args[1];

            StringBuilder mdString = new StringBuilder();
            XMLInputFactory factory = XMLInputFactory.newInstance();

            // Deal with the UTF-8 BOM problem
            BOMInputStream bomIn = new BOMInputStream(new FileInputStream(xmlPath));
            if (bomIn.hasBOM()) {
                logger.error("UTF-8 BOM detected... for more details please refer to: https://stackoverflow.com/a/20551721/5857896");
                return;
            }

            XMLEventReader eventReader = factory.createXMLEventReader( new FileReader(xmlPath) );

            logger.info("Start processing the file: " + xmlPath);

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
                                mdString.append("\n");
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
                                link = "";
                                mdString.append("]").append("(").append(link).append(")");

                            case "code":
                                mdString.append("`");
                                break;

                        }
                        break;


                }


            });

            logger.info("Conversion completed");

            Path path = Paths.get(mdDest);

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(mdString.toString());
                logger.info("Markdown saved: " + mdDest);
            }
            catch (IOException e) {
                logger.error("Error in saving the md file");
            }

        };
    }


}//End class


