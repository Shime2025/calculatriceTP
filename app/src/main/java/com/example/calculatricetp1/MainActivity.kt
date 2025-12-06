package com.example.calculatricetp1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var zoneAffichage: TextView
    private var texteAffiche = ""                  // Ce qui est affiché à l'écran
    private var premierNombre: Double? = null      // Premier nombre de l'opération
    private var operationEnCours: String? = null   // L'opération sélectionnée (+, -, ×, ÷, %)
    private var debutSaisie = true                 // Indique si on commence un nouveau nombre
    private var estUnResultat = false              // True si l'affichage est un résultat (pour la copie)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Récupération de l'état sauvegardé
        if (savedInstanceState != null) {
            texteAffiche = savedInstanceState.getString("affichage", "")
            premierNombre = if (savedInstanceState.containsKey("premierOperande")) {
                savedInstanceState.getDouble("premierOperande")
            } else null
            operationEnCours = savedInstanceState.getString("operation")
            debutSaisie = savedInstanceState.getBoolean("nouveauNombre", true)
            estUnResultat = savedInstanceState.getBoolean("estResultatCalcul", false)
        }

        zoneAffichage = findViewById(R.id.displayText)
        zoneAffichage.text = texteAffiche

        // Fonctionnalité 1: Il n'est pas possible d'entrer des valeurs directement sur la ligne du haut sans cliquer sur les boutons
        zoneAffichage.isFocusable = false
        zoneAffichage.isFocusableInTouchMode = false
        zoneAffichage.isClickable = true

        configurerBoutonsChiffres()
        configurerBoutonsOperations()
        configurerBoutonsControle()
    }

    // Fonctionnalité 8: La rotation de l'écran garde ce qui était saisi avant la rotation
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("affichage", texteAffiche)
        premierNombre?.let { outState.putDouble("premierOperande", it) }
        outState.putString("operation", operationEnCours)
        outState.putBoolean("nouveauNombre", debutSaisie)
        outState.putBoolean("estResultatCalcul", estUnResultat)
    }

    private fun configurerBoutonsChiffres() {
        val boutonsChiffres = listOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        )

        boutonsChiffres.forEach { (id, chiffre) ->
            findViewById<Button>(id).setOnClickListener {
                ajouterChiffre(chiffre)
            }
        }
    }

    private fun configurerBoutonsOperations() {
        findViewById<Button>(R.id.btnAdd).setOnClickListener { ajouterOperation("+") }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { ajouterOperation("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { ajouterOperation("×") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { ajouterOperation("÷") }
        findViewById<Button>(R.id.btnModulo).setOnClickListener { ajouterOperation("%") }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { calculerResultat() }
    }

    private fun configurerBoutonsControle() {
        findViewById<Button>(R.id.btnReset).setOnClickListener { toutEffacer() }
        findViewById<Button>(R.id.btnDelete).setOnClickListener { effacerDernier() }
        findViewById<Button>(R.id.btnNegate).setOnClickListener { inverserSigne() }
        findViewById<Button>(R.id.btnCopy).setOnClickListener { copierResultat() }
    }

    // Fonctionnalité 2: Le clic sur un chiffre l'ajoute à la fin de la ligne du haut
    private fun ajouterChiffre(chiffre: String) {
        // Si on vient de terminer un calcul ou que l'affichage est vide
        if (texteAffiche.isEmpty() || (debutSaisie && premierNombre == null)) {
            texteAffiche = chiffre
        } else {
            texteAffiche += chiffre
        }
        debutSaisie = false
        estUnResultat = false
        rafraichirAffichage()
    }

    // Fonctionnalité 3: Le clic sur une opération l'ajoute à la fin de la ligne du haut.
    private fun ajouterOperation(operation: String) {
        // Si une opération est déjà en cours, on calcule d'abord
        if (premierNombre != null && operationEnCours != null && !debutSaisie) {
            faireCalcul()
        }

        // Récupérer le nombre actuel
        val parties = texteAffiche.split(" ")
        val nombreActuel = parties.lastOrNull()?.toDoubleOrNull()

        if (nombreActuel != null) {
            premierNombre = nombreActuel
        }

        operationEnCours = operation
        debutSaisie = true

        // Ajouter l'opération à l'affichage
        if (!texteAffiche.endsWith(" ")) {
            texteAffiche += " $operation "
        } else {
            
            texteAffiche = texteAffiche.dropLast(3) + " $operation "
        }
        estUnResultat = false
        rafraichirAffichage()
    }

    // Fonctionnalité 4: Le clic sur le bouton "=" effectue l'opération choisie et affiche le résultat,
    // quand il y a une opération, sinon rien n'est modifié
    private fun calculerResultat() {
        if (premierNombre != null && operationEnCours != null && !debutSaisie) {
            faireCalcul()
        }
    }

    private fun faireCalcul() {
        val premier = premierNombre ?: return

        // Extraire le deuxième nombre
        val parties = texteAffiche.split(" ")
        val deuxieme = parties.lastOrNull()?.toDoubleOrNull() ?: return

        // Faire le calcul selon l'opération
        val resultat = when (operationEnCours) {
            "+" -> premier + deuxieme
            "-" -> premier - deuxieme
            "×" -> premier * deuxieme
            "÷" -> {
                if (deuxieme == 0.0) {
                    Toast.makeText(this, "Division par zéro impossible", Toast.LENGTH_SHORT).show()
                    toutEffacer()
                    return
                }
                premier / deuxieme
            }
            "%" -> {
                if (deuxieme == 0.0) {
                    Toast.makeText(this, "Modulo par zéro impossible", Toast.LENGTH_SHORT).show()
                    toutEffacer()
                    return
                }
                premier % deuxieme
            }
            else -> return
        }

        // Afficher le résultat sans le .0 si c'est un entier
        texteAffiche = if (resultat % 1.0 == 0.0) {
            resultat.toInt().toString()
        } else {
            resultat.toString()
        }

        premierNombre = null
        operationEnCours = null
        debutSaisie = true
        estUnResultat = true
        rafraichirAffichage()
    }

    // Fonctionnalité 5: Le clic sur le bouton de négation (moins unaire) remplace le dernier nombre saisi par son opposé
    private fun inverserSigne() {
        val parties = texteAffiche.split(" ")
        val dernierElement = parties.lastOrNull() ?: return

        val nombre = dernierElement.toDoubleOrNull() ?: return
        val oppose = -nombre

        // Formatter le nombre inversé
        val opposeStr = if (oppose % 1.0 == 0.0) {
            oppose.toInt().toString()
        } else {
            oppose.toString()
        }

        // Remplacer dans l'affichage
        if (parties.size > 1) {
            texteAffiche = parties.dropLast(1).joinToString(" ") + " " + opposeStr
        } else {
            texteAffiche = opposeStr
        }

        

        rafraichirAffichage()
    }

    // Fonctionnalité 6: Le clic sur le bouton "effacer" efface le dernier caractère que ce soit un chiffre ou une opération
    // Effacement du dernier chiffre ne doit bien sûr pas afficher un zéro
    private fun effacerDernier() {
        if (texteAffiche.isEmpty()) return

        // Si on a une opération à la fin
        if (texteAffiche.endsWith(" ")) {
            texteAffiche = texteAffiche.dropLast(3)
            operationEnCours = null
            debutSaisie = false
        } else {
            texteAffiche = texteAffiche.dropLast(1)

            // Gérer les nombres négatifs à un chiffre
            if (texteAffiche.endsWith("-")) {
                if (texteAffiche.length == 1 ||
                    (texteAffiche.length > 1 && texteAffiche[texteAffiche.length - 2] == ' ')) {
                    texteAffiche = texteAffiche.dropLast(1)
                }
            }
        }

        // Réinitialiser si tout est effacé
        if (texteAffiche.isEmpty()) {
            premierNombre = null
            operationEnCours = null
            debutSaisie = true
        } else if (texteAffiche.endsWith(" ")) {
            debutSaisie = true
        }

        estUnResultat = false
        rafraichirAffichage()
    }

    // Fonctionnalité 7: Le clic sur le bouton "reset" efface toute la ligne du haut
    private fun toutEffacer() {
        texteAffiche = ""
        premierNombre = null
        operationEnCours = null
        debutSaisie = true
        estUnResultat = false
        rafraichirAffichage()
    }

    // Fonctionnalité 9: Il est possible de copier le résultat du calcul dans le presse-papier
    private fun copierResultat() {
        if (estUnResultat && texteAffiche.isNotEmpty()) {
            copierDansPressePapier(texteAffiche)
        } else {
            Toast.makeText(this, "Aucun résultat de calcul à copier", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rafraichirAffichage() {
        zoneAffichage.text = texteAffiche
    }

    private fun copierDansPressePapier(texte: String) {
        val pressePapier = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Résultat", texte)
        pressePapier.setPrimaryClip(clip)
        Toast.makeText(this, "Résultat copié dans le presse-papier", Toast.LENGTH_SHORT).show()
    }
}
