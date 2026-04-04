package ru.example.kvstorageservice.util;

public class TarantoolConstants {

    private TarantoolConstants() {
    }

    public static final String RANGE_LUA = """
        local key_from, key_to, batch_size, iter = ...
        if key_from == nil then key_from = '' end
        if key_to == nil then key_to = string.char(0xFF) end
        if batch_size == nil or batch_size <= 0 then batch_size = 1000 end
        if iter == nil then iter = 'GE' end
        
        local result = {}
        for _, t in box.space.KV.index.primary:pairs(key_from, {iterator = iter}) do
            if t[1] > key_to then break end
            result[#result + 1] = {t[1], t[2]}
            if #result >= batch_size then break end
        end
        return unpack(result)
        """;

    public static final String GET_LUA = """
        local t = box.space.KV:get(...) if t == nil then
            return false, nil end return true, t[2]
        """;

    public static final int BATCH_SIZE = 1000;

    private static final String SPACE_NAME = "KV";

    private static final String BOX_SPACE = "return box.space.";

    public static final String PUT_LUA =
        BOX_SPACE + SPACE_NAME + ":replace({...})";

    public static final String DELETE_LUA =
        BOX_SPACE + SPACE_NAME + ":delete(...)";

    public static final String COUNT_LUA =
        BOX_SPACE + SPACE_NAME + ":count()";
}
