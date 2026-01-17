package com.github.zomboiddecompiler;

import com.github.zomboiddecompiler.rosetta.RosettaClass;
import com.github.zomboiddecompiler.rosetta.RosettaPackage;
import com.github.zomboiddecompiler.rosetta.vineflower.DefaultTypeNameProvider;
import com.github.zomboiddecompiler.rosetta.vineflower.ITypeNameProvider;
import com.github.zomboiddecompiler.rosetta.vineflower.VineflowerUtils;
import org.jetbrains.java.decompiler.struct.gen.VarType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZomboidTypeNameProvider implements ITypeNameProvider {
    private final Map<String, String> typeNameOverrides;

    public ZomboidTypeNameProvider() {
        this.typeNameOverrides = loadTypeNameOverrides();
    }

    @Override
    public String nameVar(VarType type) {
        String typeName = VineflowerUtils.getRawTypeName(type);
        
        // Check for Project Zomboid specific type overrides first
        if (typeNameOverrides.containsKey(typeName)) {
            typeName = typeNameOverrides.get(typeName);
            if (type.arrayDim > 0) {
                typeName += "s";  // Pluralize for arrays (items, vectors, etc.)
            }
            return typeName;
        }

        // Use default type naming strategy for standard types
        typeName = DEFAULT_TYPE_NAME_PROVIDER.nameVar(type);
        
        // Check for pattern-specific names (callbacks, listeners, etc.)
        String patternName = getPatternSpecificName(type, typeName);
        if (patternName != null) {
            if (type.arrayDim > 0) {
                patternName += "s";
            }
            return patternName;
        }
        
        // Handle generic types: List<String> -> list, Map<String,Integer> -> map
        if (typeName.contains("<")) {
            typeName = extractGenericBaseName(typeName);
        }
        
        // Remove common prefixes for cleaner names
        if (typeName.startsWith("iso")) {
            typeName = typeName.substring(3);
        }
        
        // Pluralize array types for better readability
        if (type.arrayDim > 0 && !typeName.endsWith("s")) {
            typeName += "s";
        }
        
        // Ensure first character is lowercase for variable names
        if (typeName.length() > 0) {
            return typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
        }
        
        return typeName;
    }
    
    /**
     * Extracts the base name from a generic type (e.g., List<String> -> list).
     */
    private static String extractGenericBaseName(String typeName) {
        int genericStart = typeName.indexOf('<');
        if (genericStart > 0) {
            return typeName.substring(0, genericStart).toLowerCase();
        }
        return typeName;
    }
    
    /**
     * Gets better variable names for common patterns like boolean predicates.
     * @param type The variable type
     * @param baseTypeName The base type name
     * @return Enhanced name for the variable, or null if no pattern matches
     */
    private static String getPatternSpecificName(VarType type, String baseTypeName) {
        // Boolean predicates and flags
        if ("boolean".equals(baseTypeName)) {
            // Could be enhanced with more context in the future
            return null;
        }
        
        // Runnable/Callable pattern
        if (baseTypeName.contains("Runnable") || baseTypeName.contains("Callable")) {
            return "task";
        }
        
        // Event/Listener pattern
        if (baseTypeName.contains("Listener") || baseTypeName.contains("Observer")) {
            return "listener";
        }
        
        // Callback pattern
        if (baseTypeName.contains("Callback")) {
            return "callback";
        }
        
        return null;
    }

    /**
     * Loads type name overrides from rosetta packages and built-in defaults.
     * Rosetta data provides domain-specific mappings; if unavailable, defaults are used.
     */
    private static Map<String, String> loadTypeNameOverrides() {
        Map<String, String> overrides = new HashMap<>();
        
        // Try to load from rosetta packages
        try {
            List<RosettaPackage> packages = ZomboidDecompiler.getResourceNamespacesStatic();
            for (RosettaPackage pkg : packages) {
                for (RosettaClass clazz : pkg.getClasses()) {
                    String qualifiedName = pkg.getName().replace(".", "/") + "/" + clazz.getName().replace(".", "$");
                    // Use class name as short form (without package)
                    String shortName = clazz.getName().toLowerCase();
                    if (!shortName.isEmpty() && !overrides.containsKey(qualifiedName)) {
                        overrides.put(qualifiedName, shortName);
                    }
                }
            }
        } catch (Exception e) {
            ZomboidDecompiler.log.log("Warning: Could not load rosetta type overrides, using defaults: " + e.getMessage());
        }
        
        // Fallback: Built-in defaults for common types
        addBuiltInDefaults(overrides);
        
        return overrides;
    }

    /**
     * Adds hardcoded type mappings as a fallback when rosetta data is unavailable.
     */
    private static void addBuiltInDefaults(Map<String, String> overrides) {
        // Spatial types
        overrides.putIfAbsent("zombie/iso/IsoGridSquare", "square");
        overrides.putIfAbsent("zombie/iso/Vector2", "vector");
        overrides.putIfAbsent("zombie/iso/Vector3", "vector");
        overrides.putIfAbsent("zombie/iso/IsoCell", "cell");
        overrides.putIfAbsent("zombie/iso/IsoChunk", "chunk");
        
        // Character and NPC types
        overrides.putIfAbsent("zombie/characters/IsoGameCharacter", "character");
        overrides.putIfAbsent("zombie/characters/IsoZombie", "zombie");
        overrides.putIfAbsent("zombie/characters/IsoPlayer", "player");
        overrides.putIfAbsent("zombie/characters/IsoSurvivor", "survivor");
        overrides.putIfAbsent("zombie/characters/IsoNPC", "npc");
        
        // Inventory system
        overrides.putIfAbsent("zombie/inventory/InventoryItem", "item");
        overrides.putIfAbsent("zombie/inventory/ItemContainer", "container");
        overrides.putIfAbsent("zombie/inventory/types/Clothing", "clothing");
        overrides.putIfAbsent("zombie/inventory/types/Food", "food");
        
        // Vehicle system
        overrides.putIfAbsent("zombie/vehicles/BaseVehicle", "vehicle");
        overrides.putIfAbsent("zombie/vehicles/VehiclePart", "part");
        overrides.putIfAbsent("zombie/vehicles/VehicleScript", "script");
        
        // Building and structures
        overrides.putIfAbsent("zombie/iso/IsoBuildingPart", "building");
        overrides.putIfAbsent("zombie/iso/IsoObject", "object");
        overrides.putIfAbsent("zombie/iso/IsoDoor", "door");
        overrides.putIfAbsent("zombie/iso/IsoWindow", "window");
        
        // Animation and graphics
        overrides.putIfAbsent("zombie/core/textures/Texture", "texture");
        overrides.putIfAbsent("zombie/iso/sprite/IsoSprite", "sprite");
        
        // Weapon types
        overrides.putIfAbsent("zombie/inventory/types/HandWeapon", "weapon");
        overrides.putIfAbsent("zombie/inventory/types/Weapon", "weapon");
        
        // Lua integration
        overrides.putIfAbsent("se/krka/kahlua/vm/KahluaTable", "table");
        overrides.putIfAbsent("se/krka/kahlua/vm/LuaTable", "luaTable");
        
        // Functional interfaces
        overrides.putIfAbsent("java/util/function/Predicate", "predicate");
        overrides.putIfAbsent("java/util/function/Function", "function");
        overrides.putIfAbsent("java/util/function/Consumer", "consumer");
        overrides.putIfAbsent("java/util/function/Supplier", "supplier");
        overrides.putIfAbsent("java/util/function/BiFunction", "biFunction");
        overrides.putIfAbsent("java/util/function/BiConsumer", "biConsumer");
        
        // Comparable and ordering
        overrides.putIfAbsent("java/util/Comparator", "comparator");
        overrides.putIfAbsent("java/lang/Comparable", "comparable");
        
        // Stream API
        overrides.putIfAbsent("java/util/stream/Stream", "stream");
        overrides.putIfAbsent("java/util/stream/Collector", "collector");
        overrides.putIfAbsent("java/util/stream/IntStream", "intStream");
        
        // Executors and threading
        overrides.putIfAbsent("java/util/concurrent/ExecutorService", "executor");
        overrides.putIfAbsent("java/util/concurrent/Executor", "executor");
        overrides.putIfAbsent("java/lang/Thread", "thread");
        overrides.putIfAbsent("java/util/concurrent/Future", "future");
        
        // Collection types - use generic base names
        overrides.putIfAbsent("java/util/ArrayList", "list");
        overrides.putIfAbsent("java/util/LinkedList", "list");
        overrides.putIfAbsent("java/util/HashMap", "map");
        overrides.putIfAbsent("java/util/LinkedHashMap", "map");
        overrides.putIfAbsent("java/util/HashSet", "set");
        overrides.putIfAbsent("java/util/LinkedHashSet", "set");
        overrides.putIfAbsent("java/util/Stack", "stack");
        overrides.putIfAbsent("java/util/Queue", "queue");
        overrides.putIfAbsent("java/util/PriorityQueue", "queue");
        overrides.putIfAbsent("java/util/ArrayDeque", "deque");
        overrides.putIfAbsent("java/util/concurrent/ConcurrentHashMap", "map");
        overrides.putIfAbsent("java/util/Collections", "collections");
        
        // I/O and streams
        overrides.putIfAbsent("java/io/InputStream", "input");
        overrides.putIfAbsent("java/io/OutputStream", "output");
        overrides.putIfAbsent("java/io/Reader", "reader");
        overrides.putIfAbsent("java/io/Writer", "writer");
        overrides.putIfAbsent("java/nio/file/Path", "path");
        overrides.putIfAbsent("java/io/File", "file");
        
        // Exceptions
        overrides.putIfAbsent("java/lang/Exception", "exception");
        overrides.putIfAbsent("java/lang/Throwable", "throwable");
        
        // Reflection
        overrides.putIfAbsent("java/lang/Class", "clazz");
        overrides.putIfAbsent("java/lang/reflect/Method", "method");
        overrides.putIfAbsent("java/lang/reflect/Field", "field");
    }

    private static final ITypeNameProvider DEFAULT_TYPE_NAME_PROVIDER = new DefaultTypeNameProvider();
}
