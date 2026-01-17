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
                typeName += "s";
            }
            return typeName;
        }

        // Use default type naming strategy for standard types
        typeName = DEFAULT_TYPE_NAME_PROVIDER.nameVar(type);
        
        // Remove common prefixes for cleaner names
        if (typeName.startsWith("iso")) {
            typeName = typeName.substring(3);
        }
        
        // Ensure first character is lowercase for variable names
        if (typeName.length() > 0) {
            return typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
        }
        
        return typeName;
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
        
        // Character and NPC types
        overrides.putIfAbsent("zombie/characters/IsoGameCharacter", "character");
        overrides.putIfAbsent("zombie/characters/IsoZombie", "zombie");
        overrides.putIfAbsent("zombie/characters/IsoPlayer", "player");
        
        // Inventory system
        overrides.putIfAbsent("zombie/inventory/InventoryItem", "item");
        overrides.putIfAbsent("zombie/inventory/ItemContainer", "container");
        
        // Vehicle system
        overrides.putIfAbsent("zombie/vehicles/BaseVehicle", "vehicle");
        overrides.putIfAbsent("zombie/vehicles/VehiclePart", "part");
        
        // Weapon types
        overrides.putIfAbsent("zombie/inventory/types/HandWeapon", "weapon");
        
        // Lua integration
        overrides.putIfAbsent("se/krka/kahlua/vm/KahluaTable", "table");
        
        // Collection types
        overrides.putIfAbsent("java/util/ArrayList", "list");
        overrides.putIfAbsent("java/util/LinkedList", "list");
        overrides.putIfAbsent("java/util/HashMap", "map");
        overrides.putIfAbsent("java/util/HashSet", "set");
        overrides.putIfAbsent("java/util/Stack", "stack");
        overrides.putIfAbsent("java/util/Queue", "queue");
    }

    private static final ITypeNameProvider DEFAULT_TYPE_NAME_PROVIDER = new DefaultTypeNameProvider();
}
