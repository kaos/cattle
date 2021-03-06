package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;

public class ScalePolicy {
    Integer increment;
    Integer min;
    Integer max;

    @Field(required = false, nullable = true, defaultValue = "1", min = 1)
    public Integer getIncrement() {
        return increment;
    }

    public void setIncrement(Integer increment) {
        this.increment = increment;
    }

    @Field(required = true, nullable = true, defaultValue = "1", min = 1)
    public Integer getMin() {
        return min;
    }

    public void setMin(Integer minScale) {
        this.min = minScale;
    }

    @Field(required = false, nullable = true, defaultValue = "1", min = 1)
    public Integer getMax() {
        return max;
    }

    public void setMax(Integer maxScale) {
        this.max = maxScale;
    }
}
