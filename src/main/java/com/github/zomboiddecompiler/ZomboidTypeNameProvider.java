package com.github.zomboiddecompiler;

import com.github.zomboiddecompiler.rosetta.vineflower.DefaultTypeNameProvider;
import com.github.zomboiddecompiler.rosetta.vineflower.ITypeNameProvider;
import com.github.zomboiddecompiler.rosetta.vineflower.VineflowerUtils;
import org.jetbrains.java.decompiler.struct.gen.VarType;

import java.util.HashMap;
import java.util.Map;

public class ZomboidTypeNameProvider implements ITypeNameProvider {
    @Override
    public String nameVar(VarType type) {
        String typeName = VineflowerUtils.getRawTypeName(type);
        
        // Check for Project Zomboid specific type overrides first
        if (TYPE_NAME_OVERRIDES.containsKey(typeName)) {
            typeName = TYPE_NAME_OVERRIDES.get(typeName);
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

    private static final Map<String, String> TYPE_NAME_OVERRIDES = createTypeOverrides();

    private static Map<String, String> createTypeOverrides() {
        Map<String, String> overrides = new HashMap<>();
        
        // Spatial types
        overrides.put("zombie/iso/IsoGridSquare", "square");
        overrides.put("zombie/iso/Vector2", "vector");
        overrides.put("zombie/iso/Vector3", "vector");
        
        // Character and NPC types
        overrides.put("zombie/characters/IsoGameCharacter", "character");
        overrides.put("zombie/characters/IsoZombie", "zombie");
        overrides.put("zombie/characters/IsoPlayer", "player");
        
        // Inventory system
        overrides.put("zombie/inventory/InventoryItem", "item");
        overrides.put("zombie/inventory/ItemContainer", "container");
        
        // Vehicle system
        overrides.put("zombie/vehicles/BaseVehicle", "vehicle");
        overrides.put("zombie/vehicles/VehiclePart", "part");
        
        // Weapon types
        overrides.put("zombie/inventory/types/HandWeapon", "weapon");
        
        // Lua integration
        overrides.put("se/krka/kahlua/vm/KahluaTable", "table");
        
        // Collection types
        overrides.put("java/util/ArrayList", "list");
        overrides.put("java/util/LinkedList", "list");
        overrides.put("java/util/HashMap", "map");
        overrides.put("java/util/HashSet", "set");
        overrides.put("java/util/Stack", "stack");
        overrides.put("java/util/Queue", "queue");
        
        return overrides;
    }

    private static final ITypeNameProvider DEFAULT_TYPE_NAME_PROVIDER = new DefaultTypeNameProvider();
}
