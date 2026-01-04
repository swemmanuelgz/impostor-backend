package com.swemmanuelgz.users.impostorbackend.utils;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Generador de palabras aleatorias en español para el juego Impostor
 * Proporciona palabras categorizadas para usar cuando el cliente no provea una
 */
@Component
public class WordGenerator {
    
    private static final Random random = new Random();
    
    /**
     * Todas las categorías disponibles
     */
    public static final List<String> CATEGORIES = List.of(
        "Animales",
        "Comida",
        "Objetos",
        "Lugares",
        "Deportes",
        "Profesiones",
        "Tecnología",
        "Naturaleza",
        "Vehículos",
        "Instrumentos"
    );
    
    /**
     * Palabras por categoría
     */
    private static final Map<String, List<String>> WORDS_BY_CATEGORY;
    
    static {
        Map<String, List<String>> words = new HashMap<>();
        
        words.put("Animales", List.of(
            "PERRO", "GATO", "ELEFANTE", "LEÓN", "TIGRE", "JIRAFA", "CEBRA",
            "CABALLO", "VACA", "OVEJA", "CERDO", "POLLO", "PATO", "ÁGUILA",
            "DELFÍN", "TIBURÓN", "BALLENA", "PINGÜINO", "OSO", "LOBO",
            "ZORRO", "CONEJO", "ARDILLA", "RATÓN", "SERPIENTE", "COCODRILO",
            "TORTUGA", "RANA", "MARIPOSA", "ABEJA", "HORMIGA", "ARAÑA",
            "GORILA", "MONO", "CAMELLO", "CANGURO", "KOALA", "PANDA"
        ));
        
        words.put("Comida", List.of(
            "PIZZA", "HAMBURGUESA", "PASTA", "ARROZ", "SUSHI", "TACOS",
            "HELADO", "CHOCOLATE", "PAN", "QUESO", "JAMÓN", "POLLO",
            "PESCADO", "ENSALADA", "SOPA", "TORTILLA", "PAELLA", "CROQUETAS",
            "PATATAS", "HUEVOS", "TOCINO", "SALCHICHA", "EMPANADA", "CHURROS",
            "GALLETAS", "PASTEL", "TARTA", "FLAN", "NATILLAS", "MANZANA",
            "NARANJA", "PLÁTANO", "FRESA", "SANDÍA", "MELÓN", "UVA"
        ));
        
        words.put("Objetos", List.of(
            "MESA", "SILLA", "SOFÁ", "CAMA", "LÁMPARA", "ESPEJO", "RELOJ",
            "TELEVISOR", "ORDENADOR", "TELÉFONO", "LIBRO", "BOLÍGRAFO",
            "CUADERNO", "MOCHILA", "GAFAS", "PARAGUAS", "MALETA", "LLAVE",
            "CARTERA", "TIJERAS", "MARTILLO", "DESTORNILLADOR", "CUCHILLO",
            "TENEDOR", "CUCHARA", "PLATO", "VASO", "TAZA", "BOTELLA",
            "NEVERA", "MICROONDAS", "LAVADORA", "PLANCHA", "ASPIRADORA"
        ));
        
        words.put("Lugares", List.of(
            "PLAYA", "MONTAÑA", "BOSQUE", "DESIERTO", "CIUDAD", "PUEBLO",
            "PARQUE", "JARDÍN", "MUSEO", "BIBLIOTECA", "CINE", "TEATRO",
            "HOSPITAL", "COLEGIO", "UNIVERSIDAD", "OFICINA", "BANCO",
            "SUPERMERCADO", "RESTAURANTE", "CAFETERÍA", "HOTEL", "AEROPUERTO",
            "ESTACIÓN", "GIMNASIO", "PISCINA", "ESTADIO", "IGLESIA",
            "PLAZA", "MERCADO", "FARMACIA", "PELUQUERÍA", "GASOLINERA"
        ));
        
        words.put("Deportes", List.of(
            "FÚTBOL", "BALONCESTO", "TENIS", "NATACIÓN", "CICLISMO",
            "ATLETISMO", "BOXEO", "KÁRATE", "JUDO", "GOLF", "BÉISBOL",
            "VOLEIBOL", "BALONMANO", "HOCKEY", "RUGBY", "SURF", "ESQUÍ",
            "SNOWBOARD", "PATINAJE", "SKATE", "ESCALADA", "SENDERISMO",
            "PESCA", "CAZA", "EQUITACIÓN", "GIMNASIA", "YOGA", "PILATES"
        ));
        
        words.put("Profesiones", List.of(
            "MÉDICO", "ENFERMERO", "ABOGADO", "INGENIERO", "ARQUITECTO",
            "PROFESOR", "COCINERO", "CAMARERO", "POLICÍA", "BOMBERO",
            "PILOTO", "CONDUCTOR", "MECÁNICO", "ELECTRICISTA", "FONTANERO",
            "CARPINTERO", "PINTOR", "ALBAÑIL", "JARDINERO", "PELUQUERO",
            "DENTISTA", "VETERINARIO", "FARMACÉUTICO", "PERIODISTA",
            "FOTÓGRAFO", "ACTOR", "MÚSICO", "CANTANTE", "BAILARÍN"
        ));
        
        words.put("Tecnología", List.of(
            "ORDENADOR", "TELÉFONO", "TABLET", "TELEVISOR", "ROUTER",
            "IMPRESORA", "TECLADO", "RATÓN", "MONITOR", "AURICULARES",
            "ALTAVOZ", "CÁMARA", "MICRÓFONO", "DISCO", "USB", "CABLE",
            "BATERÍA", "CARGADOR", "ANTENA", "SATÉLITE", "ROBOT", "DRONE",
            "CONSOLA", "VIDEOJUEGO", "INTERNET", "WIFI", "BLUETOOTH",
            "APLICACIÓN", "PROGRAMA", "SISTEMA", "BASE DE DATOS"
        ));
        
        words.put("Naturaleza", List.of(
            "ÁRBOL", "FLOR", "PLANTA", "HOJA", "RAMA", "RAÍZ", "SEMILLA",
            "BOSQUE", "SELVA", "PRADERA", "CAMPO", "RÍO", "LAGO", "MAR",
            "OCÉANO", "CASCADA", "VOLCÁN", "MONTAÑA", "COLINA", "VALLE",
            "CUEVA", "ISLA", "PENÍNSULA", "NUBE", "LLUVIA", "NIEVE",
            "TRUENO", "RELÁMPAGO", "ARCOÍRIS", "SOL", "LUNA", "ESTRELLA"
        ));
        
        words.put("Vehículos", List.of(
            "COCHE", "MOTO", "BICICLETA", "AUTOBÚS", "CAMIÓN", "FURGONETA",
            "TAXI", "AMBULANCIA", "BOMBEROS", "POLICÍA", "TREN", "METRO",
            "TRANVÍA", "AVIÓN", "HELICÓPTERO", "BARCO", "YATE", "LANCHA",
            "SUBMARINO", "COHETE", "PATINETE", "MONOPATÍN", "PATINES",
            "TRACTOR", "EXCAVADORA", "GRÚA", "CAMIONETA", "LIMUSINA"
        ));
        
        words.put("Instrumentos", List.of(
            "GUITARRA", "PIANO", "VIOLÍN", "BATERÍA", "FLAUTA", "SAXOFÓN",
            "TROMPETA", "CLARINETE", "ACORDEÓN", "ARPA", "BAJO", "UKELELE",
            "BANJO", "MANDOLINA", "ÓRGANO", "XILÓFONO", "TRIÁNGULO",
            "MARACAS", "CASTAÑUELAS", "PANDERETA", "TAMBOR", "BONGOS",
            "ARMÓNICA", "GAITA", "CELLO", "CONTRABAJO", "TUBA", "TROMBÓN"
        ));
        
        WORDS_BY_CATEGORY = Collections.unmodifiableMap(words);
    }
    
    /**
     * Obtiene una palabra aleatoria de cualquier categoría
     */
    public String getRandomWord() {
        List<String> allWords = getAllWords();
        return allWords.get(random.nextInt(allWords.size()));
    }
    
    /**
     * Obtiene una palabra aleatoria de una categoría específica
     * Si la categoría no existe, devuelve una palabra de cualquier categoría
     */
    public String getRandomWordFromCategory(String category) {
        List<String> words = WORDS_BY_CATEGORY.getOrDefault(category, getAllWords());
        return words.get(random.nextInt(words.size()));
    }
    
    /**
     * Obtiene una categoría aleatoria
     */
    public String getRandomCategory() {
        return CATEGORIES.get(random.nextInt(CATEGORIES.size()));
    }
    
    /**
     * Obtiene una palabra aleatoria junto con su categoría
     */
    public WordWithCategory getRandomWordWithCategory() {
        String category = getRandomCategory();
        String word = getRandomWordFromCategory(category);
        return new WordWithCategory(word, category);
    }
    
    /**
     * Obtiene todas las palabras de todas las categorías
     */
    private List<String> getAllWords() {
        List<String> allWords = new ArrayList<>();
        for (List<String> categoryWords : WORDS_BY_CATEGORY.values()) {
            allWords.addAll(categoryWords);
        }
        return allWords;
    }
    
    /**
     * Obtiene las palabras de una categoría
     */
    public List<String> getWordsFromCategory(String category) {
        return WORDS_BY_CATEGORY.getOrDefault(category, List.of());
    }
    
    /**
     * Obtiene todas las categorías disponibles
     */
    public List<String> getCategories() {
        return CATEGORIES;
    }
    
    /**
     * Clase para retornar palabra con su categoría
     */
    public record WordWithCategory(String word, String category) {}
}
