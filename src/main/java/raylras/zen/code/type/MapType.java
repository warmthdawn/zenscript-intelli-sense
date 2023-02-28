package raylras.zen.code.type;

public class MapType extends Type {

    public Type keyType;
    public Type valueType;

    public MapType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public Tag getTag() {
        return Tag.MAP;
    }

    @Override
    public String toString() {
        return valueType + "[" + keyType + "]";
    }

}
