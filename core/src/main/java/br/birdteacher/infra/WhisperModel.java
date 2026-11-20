package br.birdteacher.infra;

import java.net.URI;
import java.nio.file.*;
import java.util.Arrays;

public enum WhisperModel {
    SMALL_EN("small.en", "Inglês · equilibrado", 487614201, "c6138d6d58ecc8322097e0f987c32f1be8bb0a18532a3f88f734d1bbf9c41e5d"),
    MEDIUM_EN("medium.en", "Inglês · maior precisão", 1533774781, "cc37e93478338ec7700281a7ac30a10128929eb8f427dda2e865faa8f6da4356"),
    BASE_EN("base.en", "Inglês · leve", 147964211, "a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002"),
    SMALL("small", "Multilíngue · equilibrado", 487601967, "1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b"),
    MEDIUM("medium", "Multilíngue · maior precisão", 1533763059, "6c14d5adee5f86394037b4e4e8b59f1673b6cee10e3cf0b11bbdbee79c156208"),
    TURBO("large-v3-turbo", "Multilíngue · 100 idiomas", 1624555275L, "1fc70f774d38eb169993ac391eea357ef47c88757ef72ee5943879b7e8e2bc69"),
    TINY("tiny", "Multilíngue · compacto", 77691713, "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21"),
    BASE("base", "Multilíngue · leve", 147951465, "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe");

    public final String id;
    public final long bytes;
    final String sha256;
    private final String description;

    WhisperModel(String id, String description, long bytes, String sha256) {
        this.id = id;
        this.description = description;
        this.bytes = bytes;
        this.sha256 = sha256;
    }

    public String filename() { return "ggml-" + id + ".bin"; }

    URI uri() {
        return URI.create("https://huggingface.co/ggerganov/whisper.cpp/resolve/5359861c739e955e79d9a303bcbc70fb988958b1/" + filename());
    }

    boolean available(Path dir) {
        Path file = dir.resolve(filename());
        try { return Files.isRegularFile(file) && Files.size(file) == bytes; }
        catch (java.io.IOException e) { return false; }
    }

    public enum Language {
        ENGLISH("en", "Inglês"),
        CHINESE("zh", "Chinês (mandarim)"),
        GERMAN("de", "Alemão"),
        SPANISH("es", "Espanhol"),
        RUSSIAN("ru", "Russo"),
        KOREAN("ko", "Coreano"),
        FRENCH("fr", "Francês"),
        JAPANESE("ja", "Japonês"),
        PORTUGUESE("pt", "Português (Brasil)"),
        TURKISH("tr", "Turco"),
        POLISH("pl", "Polonês"),
        CATALAN("ca", "Catalão"),
        DUTCH("nl", "Holandês"),
        ARABIC("ar", "Árabe"),
        SWEDISH("sv", "Sueco"),
        ITALIAN("it", "Italiano"),
        INDONESIAN("id", "Indonésio"),
        HINDI("hi", "Hindi"),
        FINNISH("fi", "Finlandês"),
        VIETNAMESE("vi", "Vietnamita"),
        HEBREW("he", "Hebraico"),
        UKRAINIAN("uk", "Ucraniano"),
        GREEK("el", "Grego"),
        MALAY("ms", "Malaio"),
        CZECH("cs", "Tcheco"),
        ROMANIAN("ro", "Romeno"),
        DANISH("da", "Dinamarquês"),
        HUNGARIAN("hu", "Húngaro"),
        TAMIL("ta", "Tâmil"),
        NORWEGIAN("no", "Norueguês"),
        THAI("th", "Tailandês"),
        URDU("ur", "Urdu"),
        CROATIAN("hr", "Croata"),
        BULGARIAN("bg", "Búlgaro"),
        LITHUANIAN("lt", "Lituano"),
        LATIN("la", "Latim"),
        MAORI("mi", "Maori"),
        MALAYALAM("ml", "Malaiala"),
        WELSH("cy", "Galês"),
        SLOVAK("sk", "Eslovaco"),
        TELUGU("te", "Télugo"),
        PERSIAN("fa", "Persa (farsi)"),
        LATVIAN("lv", "Letão"),
        BENGALI("bn", "Bengali"),
        SERBIAN("sr", "Sérvio"),
        AZERBAIJANI("az", "Azerbaijano"),
        SLOVENIAN("sl", "Esloveno"),
        KANNADA("kn", "Canarês"),
        ESTONIAN("et", "Estoniano"),
        MACEDONIAN("mk", "Macedônio"),
        BRETON("br", "Bretão"),
        BASQUE("eu", "Basco"),
        ICELANDIC("is", "Islandês"),
        ARMENIAN("hy", "Armênio"),
        NEPALI("ne", "Nepalês"),
        MONGOLIAN("mn", "Mongol"),
        BOSNIAN("bs", "Bósnio"),
        KAZAKH("kk", "Cazaque"),
        ALBANIAN("sq", "Albanês"),
        SWAHILI("sw", "Suaíli"),
        GALICIAN("gl", "Galego"),
        MARATHI("mr", "Marata"),
        PUNJABI("pa", "Panjabi"),
        SINHALA("si", "Cingalês"),
        KHMER("km", "Khmer"),
        SHONA("sn", "Shona"),
        YORUBA("yo", "Iorubá"),
        SOMALI("so", "Somali"),
        AFRIKAANS("af", "Africâner"),
        OCCITAN("oc", "Occitano"),
        GEORGIAN("ka", "Georgiano"),
        BELARUSIAN("be", "Bielorrusso"),
        TAJIK("tg", "Tadjique"),
        SINDHI("sd", "Sindi"),
        GUJARATI("gu", "Guzerate"),
        AMHARIC("am", "Amárico"),
        YIDDISH("yi", "Iídiche"),
        LAO("lo", "Laociano"),
        UZBEK("uz", "Uzbeque"),
        FAROESE("fo", "Feroês"),
        HAITIAN_CREOLE("ht", "Crioulo haitiano"),
        PASHTO("ps", "Pastó"),
        TURKMEN("tk", "Turcomeno"),
        NYNORSK("nn", "Norueguês nynorsk"),
        MALTESE("mt", "Maltês"),
        SANSKRIT("sa", "Sânscrito"),
        LUXEMBOURGISH("lb", "Luxemburguês"),
        MYANMAR("my", "Birmanês"),
        TIBETAN("bo", "Tibetano"),
        TAGALOG("tl", "Tagalo"),
        MALAGASY("mg", "Malgaxe"),
        ASSAMESE("as", "Assamês"),
        TATAR("tt", "Tártaro"),
        HAWAIIAN("haw", "Havaiano"),
        LINGALA("ln", "Lingala"),
        HAUSA("ha", "Hauçá"),
        BASHKIR("ba", "Basquir"),
        JAVANESE("jw", "Javanês"),
        SUNDANESE("su", "Sundanês"),
        CANTONESE("yue", "Cantonês");

        public final String code;
        private final String label;
        Language(String code, String label) { this.code = code; this.label = label; }

        public boolean supports(WhisperModel model) {
            if (this == CANTONESE) return model == TURBO;
            return this == ENGLISH || !model.id.endsWith(".en");
        }

        public WhisperModel[] models() {
            return Arrays.stream(WhisperModel.values()).filter(this::supports).toArray(WhisperModel[]::new);
        }

        @Override public String toString() { return label; }
    }

    public record Profile(Language language, WhisperModel model) {
        public Profile {
            if (!language.supports(model))
                throw new IllegalArgumentException("O modelo " + model.id + " não transcreve " + language + ".");
        }
        public String preference() { return language.code + ":" + model.id; }
    }

    public static Profile restore(Path dir, String preference) {
        String[] parts = preference.split(":", 2);
        var language = parts.length == 1 ? Language.ENGLISH : Arrays.stream(Language.values())
                .filter(item -> item.code.equals(parts[0])).findFirst().orElse(Language.ENGLISH);
        String id = parts[parts.length - 1];
        var chosen = Arrays.stream(language.models()).filter(model -> model.id.equals(id)).findFirst();
        if (chosen.isPresent() && chosen.get().available(dir)) return new Profile(language, chosen.get());
        for (var model : new WhisperModel[]{MEDIUM_EN, SMALL_EN, BASE_EN, MEDIUM, SMALL, BASE, TURBO, TINY})
            if (language.supports(model) && model.available(dir)) return new Profile(language, model);
        return new Profile(language, chosen.orElse(language.models()[0]));
    }

    public static Profile englishDefault(Path dir, String preference) {
        String saved = preference.contains(":") && !preference.startsWith("en:") ? "" : preference;
        return restore(dir, saved);
    }

    public static WhisperModel startup(Path dir, String preference) {
        return restore(dir, preference).model();
    }

    public String description() { return description; }
    public String sizeLabel() {
        if (bytes >= 1073741824L)
            return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f GiB", bytes / 1073741824.0);
        return Math.round(bytes / 1048576.0) + " MiB";
    }

    @Override public String toString() {
        return id + " · " + description + " · " + sizeLabel();
    }
}
