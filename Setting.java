package com.example.knifeclient;

import java.util.Locale;

public abstract class Setting {
    public final String id;
    public final String label;

    protected Setting(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public abstract String save();
    public abstract void load(String s);
    public abstract void reset();

    public static final class Bool extends Setting {
        public boolean value;
        private final boolean def;

        public Bool(String id, String label, boolean def) {
            super(id, label);
            this.def = def;
            this.value = def;
        }

        @Override public String save() { return Boolean.toString(value); }
        @Override public void load(String s) { value = Boolean.parseBoolean(s); }
        @Override public void reset() { value = def; }
    }

    public static final class Num extends Setting {
        public static final int NORMAL = 0, HUE = 1, SAT = 2, BRI = 3;
        public double value;
        public final double min, max, step, def;
        public final String suffix;
        public int kind = NORMAL;
        public ColorGroup group;

        public Num(String id, String label, double def, double min, double max, double step, String suffix) {
            super(id, label);
            this.def = def;
            this.min = min;
            this.max = max;
            this.step = step;
            this.suffix = suffix;
            this.value = def;
        }

        public void set(double v) {
            v = Math.max(min, Math.min(max, v));
            if (step > 0) v = min + Math.round((v - min) / step) * step;
            value = Math.max(min, Math.min(max, v));
        }

        public String text() {
            if (step >= 1.0) return Math.round(value) + suffix;
            return String.format(Locale.ROOT, "%.2f", value) + suffix;
        }

        @Override public String save() { return Double.toString(value); }
        @Override public void load(String s) { set(Double.parseDouble(s)); }
        @Override public void reset() { value = def; }
    }

    public static final class Choice extends Setting {
        public final String[] options;
        public int index;
        private final int def;

        public Choice(String id, String label, int def, String... options) {
            super(id, label);
            this.options = options;
            this.def = def;
            this.index = def;
        }

        public String text() { return options[index]; }
        public void next() { index = (index + 1) % options.length; }
        public void prev() { index = (index + options.length - 1) % options.length; }

        @Override public String save() { return Integer.toString(index); }
        @Override public void load(String s) { index = Math.floorMod(Integer.parseInt(s), options.length); }
        @Override public void reset() { index = def; }
    }
}
