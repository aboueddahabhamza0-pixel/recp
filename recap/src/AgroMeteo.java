
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Locale;

public class AgroMeteo {

    // Constantes
    static final String FICHIER = "mesures_saiss.csv";
    static final double TEMP_MIN = -10.0;
    static final double TEMP_MAX = 50.0;
    static final double HUM_MIN = 0.0;
    static final double HUM_MAX = 100.0;
    static final double TBASE = 10.0;

    static final double SEUIL_GEL = 2.0;
    static final double SEUIL_CHALEUR = 30.0;
    static final double SEUIL_VENT = 35.0;
    static final double SEUIL_HUMIDITE = 40.0;

    // Compteurs
    static int lignesLues = 0;
    static int lignesAcceptees = 0;
    static int lignesRejetees = 0;

    // Lecture du fichier CSV
    static ArrayList<String[]> lireFichier() {

        ArrayList<String[]> mesures = new ArrayList<>();

        try (BufferedReader lecteur =
                     new BufferedReader(new FileReader(FICHIER))) {

            String ligne;
            int numeroLigne = 0;

            while ((ligne = lecteur.readLine()) != null) {

                numeroLigne++;

                if (numeroLigne == 1) {
                    continue;
                }

                lignesLues++;

                try {
                    String[] champs = ligne.split(",", -1);

                    if (champs.length != 7) {
                        throw new IllegalArgumentException(
                                "Nombre de champs incorrect");
                    }

                    long horodatage =
                            Long.parseLong(champs[0].trim());

                    String station = champs[1].trim();

                    double temperature =
                            Double.parseDouble(champs[2].trim());

                    double humidite =
                            Double.parseDouble(champs[3].trim());

                    double precipitation =
                            Double.parseDouble(champs[4].trim());

                    double vent =
                            Double.parseDouble(champs[5].trim());

                    int codeEtat =
                            Integer.parseInt(champs[6].trim());

                    if (!Double.isFinite(temperature)
                            || !Double.isFinite(humidite)
                            || !Double.isFinite(precipitation)
                            || !Double.isFinite(vent)
                            || precipitation < 0
                            || vent < 0) {

                        throw new IllegalArgumentException(
                                "Valeur numérique invalide");
                    }

                    if (!temperaturePlausible(temperature)
                            || !humiditePlausible(humidite)
                            || codeEtat == 3) {

                        lignesRejetees++;

                        System.err.println(
                                "Ligne " + numeroLigne
                                        + " rejetée : mesure non plausible "
                                        + "ou panne.");

                        continue;
                    }

                    mesures.add(new String[]{
                            Long.toString(horodatage),
                            station,
                            Double.toString(temperature),
                            Double.toString(humidite),
                            Double.toString(precipitation),
                            Double.toString(vent),
                            Integer.toString(codeEtat)
                    });

                    lignesAcceptees++;

                } catch (IllegalArgumentException e) {

                    lignesRejetees++;

                    System.err.println(
                            "Ligne " + numeroLigne
                                    + " rejetée : " + e.getMessage());
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "Erreur de lecture du fichier : "
                            + e.getMessage());
        }

        return mesures;
    }

    // Validation de la température
    static boolean temperaturePlausible(double temperature) {
        return temperature >= TEMP_MIN
                && temperature <= TEMP_MAX;
    }

    // Validation de l'humidité
    static boolean humiditePlausible(double humidite) {
        return humidite >= HUM_MIN
                && humidite <= HUM_MAX;
    }

    // Traduction du code d'état
    static String libelleEtat(int code) {

        switch (code) {
            case 1:
                return "Normal";
            case 2:
                return "Suspecte";
            case 3:
                return "Panne";
            default:
                return "Code inconnu";
        }
    }

    // Moyenne d'un tableau
    static double moyenne(double[] valeurs) {

        if (valeurs.length == 0) {
            return 0.0;
        }

        double somme = 0.0;

        for (double valeur : valeurs) {
            somme += valeur;
        }

        return somme / valeurs.length;
    }

    // Surcharge : moyenne de deux valeurs
    static double moyenne(double a, double b) {
        return (a + b) / 2.0;
    }

    // Détermination du risque
    static String risque(
            double temperature,
            double vent,
            double humidite) {

        if (temperature < SEUIL_GEL) {
            return "Risque de gel";

        } else if (temperature > SEUIL_CHALEUR
                && vent > SEUIL_VENT
                && humidite < SEUIL_HUMIDITE) {

            return "Risque d'incendie";

        } else if (temperature > SEUIL_CHALEUR) {
            return "Stress thermique";

        } else {
            return "Conditions normales";
        }
    }

    // Rapport d'une station
    static void afficherRapport(
            ArrayList<String[]> mesures,
            String nomStation) {

        double minimum = Double.MAX_VALUE;
        double maximum = -Double.MAX_VALUE;
        double somme = 0.0;
        double pluie = 0.0;
        int nombre = 0;

        System.out.println("\n===== Rapport : "
                + nomStation + " =====");

        for (String[] m : mesures) {

            if (!m[1].equalsIgnoreCase(nomStation)) {
                continue;
            }

            double temperature = Double.parseDouble(m[2]);
            double humidite = Double.parseDouble(m[3]);
            double precipitation = Double.parseDouble(m[4]);
            double vent = Double.parseDouble(m[5]);

            int codeEtat = Integer.parseInt(m[6]);

            minimum = Math.min(minimum, temperature);
            maximum = Math.max(maximum, temperature);
            somme += temperature;
            pluie += precipitation;
            nombre++;

            System.out.printf(Locale.US,
                    "Température : %.2f °C | État : %s | Risque : %s%n",
                    temperature,
                    libelleEtat(codeEtat),
                    risque(temperature, vent, humidite));
        }

        if (nombre == 0) {
            System.out.println(
                    "Station inexistante ou sans mesure.");
            return;
        }

        double moyenneC = somme / nombre;
        double moyenneF = moyenneC * 9.0 / 5.0 + 32.0;
        double amplitude = maximum - minimum;

        double djc = Math.max(
                0.0,
                moyenne(minimum, maximum) - TBASE);

        System.out.println("\n--- Indicateurs ---");

        System.out.println("Mesures retenues : " + nombre);

        System.out.printf(Locale.US,
                "Température minimale : %.2f °C%n", minimum);

        System.out.printf(Locale.US,
                "Température maximale : %.2f °C%n", maximum);

        System.out.printf(Locale.US,
                "Amplitude thermique : %.2f °C%n", amplitude);

        System.out.printf(Locale.US,
                "Température moyenne : %.2f °C%n", moyenneC);

        System.out.printf(Locale.US,
                "Température moyenne : %.2f °F%n", moyenneF);

        System.out.printf(Locale.US,
                "Précipitations cumulées : %.2f mm%n", pluie);

        System.out.printf(Locale.US,
                "Degrés-jours de croissance : %.2f%n", djc);
    }

    // Affichage des alertes
    static void afficherAlertes(ArrayList<String[]> mesures) {

        System.out.println("\n===== Alertes =====");

        boolean alerteTrouvee = false;

        for (String[] m : mesures) {

            double temperature = Double.parseDouble(m[2]);
            double humidite = Double.parseDouble(m[3]);
            double vent = Double.parseDouble(m[5]);

            String niveau = risque(temperature, vent, humidite);

            if (!niveau.equals("Conditions normales")) {

                System.out.println(
                        m[1] + " : " + niveau
                                + " (température : "
                                + temperature + " °C)");

                alerteTrouvee = true;
            }
        }

        if (!alerteTrouvee) {
            System.out.println("Aucune alerte.");
        }
    }

    // Bilan de qualité
    static void afficherQualite() {

        System.out.println("\n===== Bilan de qualité =====");
        System.out.println("Lignes lues : " + lignesLues);
        System.out.println("Lignes acceptées : " + lignesAcceptees);
        System.out.println("Lignes rejetées : " + lignesRejetees);

        double taux = 0.0;

        if (lignesLues > 0) {
            taux = lignesAcceptees * 100.0 / lignesLues;
        }

        System.out.printf(Locale.US,
                "Taux d'acceptation : %.2f%%%n", taux);
    }

    // Conversion des températures
    static void convertirTemperature(Scanner sc) {

        System.out.println("1. Celsius vers Fahrenheit");
        System.out.println("2. Fahrenheit vers Celsius");
        System.out.print("Votre choix : ");

        try {
            int choix = Integer.parseInt(sc.nextLine());

            System.out.print("Saisir la température : ");

            double temperature =
                    Double.parseDouble(sc.nextLine());

            if (!Double.isFinite(temperature)) {
                System.out.println("Température invalide.");
                return;
            }

            switch (choix) {
                case 1:
                    System.out.printf(Locale.US,
                            "%.2f °C = %.2f °F%n",
                            temperature,
                            temperature * 9.0 / 5.0 + 32.0);
                    break;

                case 2:
                    System.out.printf(Locale.US,
                            "%.2f °F = %.2f °C%n",
                            temperature,
                            (temperature - 32.0) * 5.0 / 9.0);
                    break;

                default:
                    System.out.println("Choix invalide.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Veuillez saisir un nombre valide.");
        }
    }

    // Méthode principale
    public static void main(String[] args) {

        ArrayList<String[]> mesures = lireFichier();

        Scanner sc = new Scanner(System.in);
        int choix;

        do {
            System.out.println("\n===== AgroMétéo Saïss =====");
            System.out.println("1. Afficher le rapport d'une station");
            System.out.println("2. Afficher toutes les alertes");
            System.out.println("3. Convertir une température");
            System.out.println("4. Afficher le bilan de qualité");
            System.out.println("0. Quitter");

            System.out.print("Votre choix : ");

            try {
                choix = Integer.parseInt(sc.nextLine());

                switch (choix) {

                    case 1:
                        System.out.print(
                                "Nom de la station : ");

                        String nom = sc.nextLine().trim();

                        afficherRapport(mesures, nom);
                        break;

                    case 2:
                        afficherAlertes(mesures);
                        break;

                    case 3:
                        convertirTemperature(sc);
                        break;

                    case 4:
                        afficherQualite();
                        break;

                    case 0:
                        System.out.println("Au revoir !");
                        break;

                    default:
                        System.out.println(
                                "Choix invalide. Réessayez.");
                }

            } catch (NumberFormatException e) {

                choix = -1;

                System.out.println(
                        "Veuillez saisir un nombre valide.");
            }

        } while (choix != 0);

        sc.close();
    }
}
