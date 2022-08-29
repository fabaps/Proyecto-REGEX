import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regexp {
    public static void main(String[] args) throws Exception {
        // Obtener parametros
        String path = args[0];
        String mode = args[1].substring(1);

        // Leer archivo de entrada
        String content = Tools.readFile(path);

        // Seleccionar modo
        Grammar grammar = null;
        if (mode.equals("gld"))
            grammar = new GLD(content);
        if (mode.equals("eval"))
            grammar = new Eval(content);

        // Error modo no encontrado
        if (grammar == null)
            throw new Exception("Grammatica no encontrada");

        // Error de eval
        if (mode.equals("eval"))
            grammar.parse();
        else {
            String outName = args[2];
            Tools.writeFile("./" + outName.concat(grammar.getExtension()), grammar.parse());
        }
    }
}

abstract class Grammar {
    // Iniciales
    public String content;
    public int stateCounter = 0;

    // Estados (ignoramos el "S" por que es un estado inicial)
    public String[] states = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "T", "U", "V", "W", "X", "Y", "Z", };

    /**
     * Iniciar variables locales
     * 
     * @param content Contenido del archivo de entrada
     */
    public Grammar(String content) {
        this.content = content;
    }

    /**
     * Dada una cadena, devuelve una gramatica.
     * 
     * @return contenido de Gramatica
     */
    public abstract String parse();

    /**
     * Devuelve la extension del archivo.
     * 
     * @return La extension del archivo.
     */
    public abstract String getExtension();
}

class Tools {
    // SIMBOLOS
    public static String plus = Pattern.quote("+");
    public static String star = Pattern.quote("*");
    public static String openParenthesis = Pattern.quote("(");
    public static String closeParenthesis = Pattern.quote(")");

    /**
     * Crea un objeto de escaner, imprime un mensaje en la consola,
     * lee la siguiente linea de entrada,
     * cierra el escaner y devuelve la entrada
     * 
     * @return La entrada del usuario.
     */
    public static String readInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese una cadena: ");

        // Leer entrada
        if (scanner.hasNext()) {
            String input = scanner.next();
            scanner.close();
            return input;
        }

        // Cerrar escaner
        scanner.close();
        return "";
    }

    /**
     * Lee un archivo y devuelve su contenido como una cadena
     * 
     * @param path La ruta al archivo que desea leer.
     * @return El contenido del archivo.
     */
    public static String readFile(String path) {
        // Leer archivo como string
        Path filePath = Path.of(path);
        String content = "";

        // Leer archivo
        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Contenido
        return content;
    }

    /**
     * Toma una cadena y reemplaza todos los parentesis de apertura y cierre con una
     * cadena vacia
     * 
     * @param str La cadena a modificar
     * @return La cadena con el parentesis eliminado.
     */
    public static String removeParenthesis(String str) {
        return str.replaceAll(openParenthesis, "").replaceAll(closeParenthesis, "");
    }

    /**
     * Toma una ruta y una cadena, y escribe la cadena en el archivo en la ruta
     * 
     * @param path    La ruta al archivo.
     * @param content el contenido para escribir en el archivo
     */
    public static void writeFile(String path, String content) {
        // Ruta
        Path filePath = Path.of(path);

        // Escribir archivo
        try {
            Files.writeString(filePath, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class AFD extends Grammar {
    // Globales
    private String regexp;

    public AFD(String content) {
        super(content);
    }

    @Override
    public String parse() {
        // Datos de regexp
        String terminals = this.content.substring(0, this.content.indexOf("\n")) + "\n";
        regexp = this.content.substring(this.content.indexOf("\n") + 1).trim();

        return null;
    }

    @Override
    public String getExtension() {
        return ".adf";
    }

}

class Eval extends Grammar {
    public Eval(String content) {
        super(content);
    }

    @Override
    public String parse() {
        // Iniciales
        String regexp = this.content.substring(this.content.indexOf("\n") + 1).trim();

        // Leer input
        String input = Tools.readInput();

        // Buscar
        Pattern flatRegex = Pattern.compile(regexp.replaceAll(Tools.plus, "|"));
        Matcher matcher = flatRegex.matcher(input);

        // Match
        if (matcher.find())
            if (matcher.matches()) {
                System.out.println("Cadena aceptada");
                return "";
            }

        System.out.println("Cadena rechazada");
        return "";
    }

    @Override
    public String getExtension() {
        return "";
    }
}

class GLD extends Grammar {
    // Globales
    private String regexp;

    public GLD(String content) {
        super(content);
    }

    /**
     * Encuentra todos los patrones OR en las reglas y los reemplaza con un nuevo
     * estado
     * 
     * @return El metodo devuelve una cadena con las reglas del patron or.
     */
    public String getOrPatterns() {
        // Salida de patron or
        String orOut = "";
        while (regexp.contains("+")) {
            // Regexp de OR
            Map<String, String> orChars = new HashMap<>();
            Pattern orRegex = Pattern
                    .compile(Tools.openParenthesis + "?([a-zA-Z_0-9]" + "(?:" + Tools.plus + "?[a-zA-Z_0-9])+)"
                            + Tools.closeParenthesis + "?|"
                            + Tools.openParenthesis + "([a-zA-Z_0-9]+)" +
                            Tools.closeParenthesis + Tools.plus);
            Matcher orMatches = orRegex.matcher(regexp);

            // Buscar patron OR
            while (orMatches.find()) {
                String group = orMatches.group();
                String[] groups = Tools.removeParenthesis(group).split(Tools.plus);

                // Validar estados repetidos
                if (orChars.containsValue(group))
                    continue;

                // Agregar patron
                orChars.put(states[stateCounter], group);
                for (String g : groups) {
                    orOut += states[stateCounter] + " -> " + g + "\n";
                }

                // Iterar reglas
                regexp = regexp.replaceAll(Pattern.quote(group), states[stateCounter]);
                stateCounter++;
            }
        }
        return orOut;
    }

    /**
     * Encuentra todos los patrones de Kleen en las reglas y los reemplaza con un
     * nuevo estado
     * 
     * @return Una cuerda con los patrones de Kleen.
     */
    public String getKleenPatterns() {
        // Patron Kleen
        String kleenOut = "";
        Map<String, String> kleenChars = new HashMap<>();
        Pattern kleenRegex = Pattern
                .compile(Tools.openParenthesis + "?([a-zA-Z_0-9]+" + Tools.closeParenthesis + "?)" + Tools.star);
        Matcher kleenMatches = kleenRegex.matcher(regexp);

        // Buscar patron Kleen
        while (kleenMatches.find()) {
            String group = kleenMatches.group();
            String[] groups = Tools.removeParenthesis(group).split(Tools.star);

            // Validar estados repetidos
            if (kleenChars.containsValue(group))
                continue;

            // Agregar patron
            kleenChars.put(states[stateCounter], group);

            // Agregar producciones
            kleenOut += states[stateCounter] + " -> " + groups[0] + states[stateCounter] + "\n";
            kleenOut += states[stateCounter] + " -> " + "\n";

            // Agregar reglas
            regexp = regexp.replaceAll(Pattern.quote(group), states[stateCounter]);
        }

        stateCounter++;
        return kleenOut;
    }

    /**
     * Devuelve una cadena de todos los estados en la matriz.
     * 
     * @return Se devuelve la variable totalStates.
     */
    public String getTotalStates() {
        String totalStates = "";
        for (int i = 0; i < stateCounter - 1; i++)
            totalStates += states[i] + ",";

        return "S," + totalStates.substring(0, totalStates.length() - 1) + "\n";
    }

    public String parse() {
        // Datos de regexp
        String terminals = this.content.substring(0, this.content.indexOf("\n")) + "\n";
        regexp = this.content.substring(this.content.indexOf("\n") + 1).trim();

        // Salida como gramatica
        String orPatterns = getOrPatterns();
        String kleenPatterns = getKleenPatterns();
        String nonTerminals = getTotalStates();
        String initialState = "S\n" + "S -> " + regexp + "\n";

        // Formato de salida
        String grammarContent = nonTerminals + terminals + initialState + orPatterns + kleenPatterns;
        String finalOut = Tools.removeParenthesis(grammarContent.substring(0, grammarContent.length() - 1));
        return finalOut;
    }

    public String getExtension() {
        return ".gld";
    }
}
