package com.github.zomboiddecompiler.rosetta;

import org.json.JSONObject;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@SuppressWarnings("unchecked") public class RosettaParser {
    /// Accumulated list of parsed rosetta packages.
    public final List<RosettaPackage> packages = new ArrayList<>();

    /**
     * Recursively scans a directory for .json and .yml rosetta files and parses them.
     * @param directory The root directory to scan for rosetta files.
     */
    public void parseDirectory(Path directory) {
        assert Files.exists(directory) && Files.isDirectory(directory);

        try (Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.toList()) {
                String fileName = file.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".json")) {
                    try {
                        parseJson(Files.newInputStream(file));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else if (fileName.endsWith(".yml")) {
                    try {
                        parseYaml(Files.newInputStream(file));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses a JSON-formatted rosetta file from the given input stream.
     * @param stream The input stream containing JSON data.
     * @return True if parsing succeeded, false otherwise.
     */
    public boolean parseJson(InputStream stream) {
        StringBuilder source = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            while(reader.ready()) {
                source.append((char)reader.read());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        String sourceString = source.toString().replace("\n", "");
        Map<String, Object> json = new JSONObject(sourceString).toMap();
        return this.parseFile(json);
    }

    /**
     * Parses a YAML-formatted rosetta file from the given input stream.
     * @param stream The input stream containing YAML data.
     * @return True if parsing succeeded, false otherwise.
     */
    public boolean parseYaml(InputStream stream) {
        Map<String, Object> yaml = (Map<String, Object>)RosettaParser.yaml.loadFromInputStream(stream);
        return this.parseFile(yaml);
    }

    private static final Load yaml = new Load(LoadSettings.builder().build());

    /**
     * Parses a rosetta file (JSON or YAML) into packages and adds them to the packages list.
     * Validates version (1.1) and language (java) presence before parsing.
     * @param raw The parsed data structure (Map) containing rosetta content.
     * @return True if file structure is valid and was successfully parsed, false otherwise.
     */
    private boolean parseFile(Map<String, Object> raw) {
        if (!raw.containsKey("version")
                || !raw.get("version").equals("1.1")
                || !raw.containsKey("languages")) {
            return false;
        }
        Map<String, Object> languages = (Map<String, Object>)raw.get("languages");

        if (!languages.containsKey("java")) {
            return false;
        }
        Map<String, Object> java = (Map<String, Object>)languages.get("java");

        if (!java.containsKey("packages")) {
            return false;
        }
        Map<String, Map<String, Object>> packages = (Map<String, Map<String, Object>>)java.get("packages");

        for (Map.Entry<String, Map<String, Object>> pkg : packages.entrySet()) {
            this.packages.add(parsePackage(pkg.getValue(), pkg.getKey()));
        }

        return true;
    }

    /**
     * Extracts the type name from a type definition, including support for generic types.
     * @param raw Map containing the type definition with a 'basic' key and optional generic type parameters.
     * @return The type name as a string, optionally including generic parameters (e.g., "List<String>").
     */
    private String parseType(Map<String, Object> raw) {
        String basicType = (String)raw.get("basic");
        
        // Check for generic type parameters
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> generics = (List<Map<String, Object>>)raw.get("generics");
        if (generics != null && !generics.isEmpty()) {
            StringBuilder typeBuilder = new StringBuilder(basicType).append("<");
            for (int i = 0; i < generics.size(); i++) {
                if (i > 0) {
                    typeBuilder.append(", ");
                }
                typeBuilder.append(parseType(generics.get(i)));
            }
            typeBuilder.append(">");
            return typeBuilder.toString();
        }
        
        return basicType;
    }

    /**
     * Parses a package entry and its contained classes into a RosettaPackage object.
     * @param raw Map containing class definitions keyed by class name.
     * @param name The package name.
     * @return A RosettaPackage object containing all parsed classes.
     */
    private RosettaPackage parsePackage(Map<String, Object> raw, String name) {
        RosettaPackage rosettaPackage = new RosettaPackage(name);

        for(String clazzName : raw.keySet()) {
            rosettaPackage.addClass(
                    parseClass((Map<String, Object>)raw.get(clazzName), clazzName)
            );
        }

        return rosettaPackage;
    }

    /**
     * Parses a class definition including its methods, fields, and constructors.
     * @param raw Map containing the class definition with keys like 'methods', 'fields', 'constructors'.
     * @param name The class name.
     * @return A RosettaClass object containing all parsed members.
     */
    private RosettaClass parseClass(Map<String, Object> raw, String name) {
        RosettaClass rosettaClass = new RosettaClass(name);

        List<Map<String, Object>> methods = (List<Map<String, Object>>)raw.get("methods");
        if (methods != null) {
            for (Map<String, Object> method : methods) {
                rosettaClass.addMethod(
                        parseMethod(method)
                );
            }
        }

        List<Map<String, Object>> staticMethods = (List<Map<String, Object>>)raw.get("staticMethods");
        if (staticMethods != null) {
            for (Map<String, Object> staticMethod : staticMethods) {
                rosettaClass.addMethod(
                        parseMethod(staticMethod)
                );
            }
        }

        Map<String, Map<String, Object>> fields = (Map<String, Map<String, Object>>)raw.get("fields");
        if (fields != null) {
            parseFieldArray(fields, rosettaClass);
        }

        Map<String, Map<String, Object>> staticFields = (Map<String, Map<String, Object>>)raw.get("staticFields");
        if (staticFields != null) {
            parseFieldArray(staticFields, rosettaClass);
        }

        List<Map<String, Object>> constructors = (List<Map<String, Object>>)raw.get("constructors");
        if (constructors != null) {
            for (Map<String, Object> constructor : constructors) {
                rosettaClass.addConstructor(
                        parseConstructor(constructor)
                );
            }
        }

        rosettaClass.setNotes((String)raw.getOrDefault("notes", ""));
        rosettaClass.setDeprecated((boolean)raw.getOrDefault("deprecated", false));

        return rosettaClass;
    }

    /**
     * Parses a collection of field definitions and adds them to the provided RosettaClass.
     * @param fields Map of field definitions keyed by field name.
     * @param rosettaClass The RosettaClass to add parsed fields to.
     */
    private void parseFieldArray(Map<String, Map<String, Object>> fields, RosettaClass rosettaClass) {
        if (fields.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Map<String, Object>> entry: fields.entrySet()) {
            Map<String, Object> field = entry.getValue();

            RosettaField rosettaField = new RosettaField((String)field.get("name"),
                                                         parseType((Map<String, Object>)field.get("type")));
            rosettaField.setNotes((String)field.getOrDefault("notes", ""));
            rosettaField.setDeprecated((boolean)field.getOrDefault("deprecated", false));

            rosettaClass.addField(rosettaField);
        }
    }

    /**
     * Parses a method return type specification.
     * @param returns Map containing the return type definition.
     * @return A RosettaReturn object representing the return type, or RosettaReturn.VOID if type is void.
     */
    private RosettaReturn parseReturn(Map<String, Object> returns) {
        String type = parseType((Map<String, Object>)returns.get("type"));
        if (type.equalsIgnoreCase("void")) {
            return RosettaReturn.VOID;
        }
        
        RosettaReturn rosettaReturn = new RosettaReturn(
                (String)returns.getOrDefault("name", ""),
                parseType((Map<String, Object>)returns.get("type"))
        );

        rosettaReturn.setNotes((String)returns.getOrDefault("notes", ""));

        return rosettaReturn;
    }

    /**
     * Parses a list of method/constructor parameters and adds them to the provided executable.
     * @param executable The RosettaExecutable (method or constructor) to add parameters to.
     * @param parameters List of parameter definitions to parse.
     */
    private void parseParameters(RosettaExecutable executable, List<Map<String, Object>> parameters) {
        for (Map<String, Object> parameter : parameters) {
            RosettaParameter rosettaParameter = new RosettaParameter(
                    (String)parameter.getOrDefault("name", ""),
                    parseType((Map<String, Object>)parameter.get("type"))
            );

            rosettaParameter.setNotes((String)parameter.getOrDefault("notes", ""));

            executable.addParameter(rosettaParameter);
        }
    }

    /**
     * Parses a method definition including name, return type, parameters, and modifiers.
     * @param method Map containing the method definition.
     * @return A RosettaMethod object with all parsed details.
     */
    private RosettaMethod parseMethod(Map<String, Object> method) {
        RosettaMethod rosettaMethod = new RosettaMethod(
                (String)method.get("name"), parseReturn((Map<String, Object>)method.get("return")));

        List<Map<String, Object>> parameters = (List<Map<String, Object>>)method.get("parameters");
        if (parameters != null) {
            parseParameters(rosettaMethod, parameters);
        }

        List<String> modifiers = (List<String>)method.get("modifiers");
        if (modifiers != null && modifiers.contains("static")) {
            rosettaMethod.setStatic(true);
        }

        rosettaMethod.setNotes((String)method.getOrDefault("notes", ""));
        rosettaMethod.setDeprecated((boolean)method.getOrDefault("deprecated", false));

        return rosettaMethod;
    }

    /**
     * Parses a constructor definition including parameters and metadata.
     * @param constructor Map containing the constructor definition.
     * @return A RosettaConstructor object with all parsed details.
     */
    private RosettaConstructor parseConstructor(Map<String, Object> constructor) {
        RosettaConstructor rosettaConstructor = new RosettaConstructor();

        List<Map<String, Object>> parameters = (List<Map<String, Object>>)constructor.get("parameters");
        if (parameters != null) {
            parseParameters(rosettaConstructor, parameters);
        }

        rosettaConstructor.setNotes((String)constructor.getOrDefault("notes", ""));
        rosettaConstructor.setDeprecated((boolean)constructor.getOrDefault("deprecated", false));

        return rosettaConstructor;
    }
}
