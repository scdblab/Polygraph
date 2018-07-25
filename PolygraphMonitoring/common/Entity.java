public class Entity {
    protected String key;
    protected String name;
    protected Property[] properties;

    public Entity(String key, String name, Property[] properties) {
        this.key = key;
        this.name = name;
        this.properties = properties;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(Property[] properties) {
        this.properties = properties;
    }

    public Entity getCopy() {
        Property[] newPA = new Property[this.properties.length];
        for (int i = 0; i < properties.length; i++)
            newPA[i] = properties[i].getCopy();
        Entity result = new Entity(key, name, newPA);
        return result;
    }

    public boolean same(Entity e) {
        if (properties.length != e.properties.length)
            return false;
        for (int i = 0; i < properties.length; i++) {
            if (!properties[i].getValue().equals(e.properties[i].getValue())) {
                return false;
            }
        }
        return true;
    }

    public String getEntityKey() {
        return PolygraphHelper.concatWithSeperator(PolygraphHelper.KEY_SEPERATOR, name, key);
    }

}
