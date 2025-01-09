package com.luismi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismi.model.Generation;
import com.luismi.model.PokemonData;
import com.luismi.model.Starter;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedOutput;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.context.Context;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {

        // https://belief-driven-design.com/thymeleaf-part-1-basics-3a1d9/
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Properties properties = new Properties();
        try (InputStreamReader input = new InputStreamReader(new FileInputStream("src/main/resources/config.ini"))) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String name = properties.getProperty("name");
        String description = properties.getProperty("description");

        // https://github.com/everit-org/json-schema
        PokemonData pokemonData;
        try (InputStream schemaStream = new FileInputStream("src/main/resources/pokemon-generations-schema.json");
             InputStream jsonStream = new FileInputStream("src/main/resources/pokemon-generations.json")) {

            JSONObject jsonSchema = new JSONObject(new JSONTokener(schemaStream));
            JSONObject jsonData = new JSONObject(new JSONTokener(jsonStream));

            Schema schema = SchemaLoader.load(jsonSchema);
            schema.validate(jsonData);

            ObjectMapper objectMapper = new ObjectMapper();
            pokemonData = objectMapper.readValue(jsonData.toString(), PokemonData.class);

        } catch (IOException | ValidationException e) {
            throw new RuntimeException(e);
        }

        Context context = new Context();
        context.setVariable("generations", pokemonData.pokemonGenerations());
        context.setVariable("name", name);
        context.setVariable("description", description);

        String htmlContent = templateEngine.process("templateGenerations", context);
        writeHtml(htmlContent, "src/main/resources/generated/index.html");

        for (Generation pokemonGeneration : pokemonData.pokemonGenerations()) {
            Context detailsContext = new Context();
            detailsContext.setVariable("starters", pokemonGeneration.starters());
            detailsContext.setVariable("name", name);
            detailsContext.setVariable("description", description);
            String htmlDetails = templateEngine.process("templateStarters", detailsContext);
            String fileName = "src/main/resources/generated/details_" + pokemonGeneration.generationNum() + ".html";
            writeHtml(htmlDetails, fileName);
        }

        generateRSS(pokemonData, "src/main/resources/generated/rss.xml", name, description);

    }

    public static void writeHtml(String htmlContent, String fileName) {

        try {
            Files.createDirectories(Paths.get("src/main/resources/generated"));
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(htmlContent);
            writer.close();
        } catch (IOException e) {
            System.err.println("ERROR: writeHtml");
        }

    }

    // https://www.youtube.com/watch?v=6HNUqDL-pI8
    // https://www.blackslate.io/articles/create-rss-feeds-in-java-using-rome
    public static void generateRSS(PokemonData pokemonData, String path, String name, String description) {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle(name);
        feed.setLink("src/main/resources/generated/index.html");
        feed.setDescription(description);

        List<SyndEntry> entries = new ArrayList<>();
        for (Generation generation : pokemonData.pokemonGenerations()) {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(generation.generationName());
            entry.setLink("src/main/resources/generated/details_" + generation.generationNum() + ".html");

            SyndContent content = new SyndContentImpl();
            StringBuilder startersDescription = new StringBuilder("Starters: ");
            for (Starter starter : generation.starters()) {
                startersDescription.append(starter.starterName()).append(" (").append(starter.starterType()).append("), ");
            }
            startersDescription.setLength(startersDescription.length() - 2); // Remove last comma and space
            content.setValue(startersDescription.toString());
            entry.setDescription(content);

            entries.add(entry);
        }
        feed.setEntries(entries);

        try {
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, new FileWriter(path));
        } catch (Exception e) {
            System.err.println("ERROR: generateRSS");
        }

    }

}