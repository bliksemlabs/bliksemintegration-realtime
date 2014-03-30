package nl.ovapi.bison.model;
public enum SubReasonType{
	Eerdere_verstoring,Snelheidsbeperkingen,Logistieke_problemen,Werkzaamheden,Ongeval,Aanrijding,File,Omgevallen_bomen,Extreme_drukte,
	Passagier_onwel,Vee_op_de_route,Mensen_op_de_route,Bommelding,Brand,Tweede_wereldoorlog_bom,Bloemencorso,Braderie,Carnaval,Jaarmarkt,
	Kermis,Koninginnedag,Marathon,Optocht,Wielerronde,Last_van_de_Brandweer,Last_van_de_Politie,Ontruiming,Stremming,
	Onbekend,Voetbalwedstrijd,Aanrijding_met_Persoon,Auto_in_spoor,Rioleringswerkzaamheden,Wegwerkzaamheden,Asfalteringswerkzaamheden,
	Bestratingswerkzaamheden,Route_versperd,Herdenking,Avondvierdaagse,Staking,Tekort_aan_personeel,Vakbondsacties,Stiptheidsacties,Mogelijke_staking,
	Defect_materieel,Defecte_brug,Defect_viaduct,Tekort_aan_materieel,Defecte_trein,Defecte_bovenleiding,Defect_spoor,Ontsporing,Seinstoring,Wisselstoring,
	Stroomstoring,Overwegstoring,Sein_en_wisselstoring,Storing_in_verkeersleidingssysteem,Gladde_sporen,Uitloop_werkzaamheden,Herstel_werkzaamheden,Uitloop_herstel_werkzaamheden,
	Gladheid,IJsgang,Sneeuw,Wateroverlast,Storm,IJzel,Blikseminslag,NULL;

	public static SubReasonType parse(String value){
		if (value == null)
			return null;
		if ("0_1".equals(value))
			return Eerdere_verstoring;
		else if ("26_1".equals(value))
			return Snelheidsbeperkingen;
		else if ("26_2".equals(value))
			return Logistieke_problemen;
		else if ("23".equals(value))
			return Werkzaamheden;
		else if ("6_6".equals(value))
			return Aanrijding;
		else if ("15".equals(value))
			return File;
		else if ("19_1".equals(value))
			return Omgevallen_bomen;
		else if ("7".equals(value))
			return Extreme_drukte;
		else if ("6_4".equals(value))
			return Passagier_onwel;
		else if ("20".equals(value))
			return Vee_op_de_route;
		else if ("17".equals(value))
			return Mensen_op_de_route;
		else if ("3_9".equals(value))
			return Bommelding;
		else if ("4".equals(value))
			return Brand;
		else if ("3_15".equals(value))
			return Tweede_wereldoorlog_bom;
		else if ("24_6".equals(value))
			return Bloemencorso;
		else if ("24_7".equals(value))
			return Braderie;
		else if ("24_8".equals(value))
			return Carnaval;
		else if ("24_9".equals(value))
			return Jaarmarkt;
		else if ("24_10".equals(value))
			return Kermis;
		else if ("24_11".equals(value))
			return Koninginnedag;
		else if ("24_12".equals(value))
			return Marathon;
		else if ("24_1".equals(value))
			return Optocht;
		else if ("24_13".equals(value))
			return Wielerronde;
		else if ("3_17".equals(value))
			return Last_van_de_Brandweer;
		else if ("3_1".equals(value))
			return Last_van_de_Politie;
		else if ("3_11".equals(value))
			return Ontruiming;
		else if ("6_3".equals(value))
			return Aanrijding_met_Persoon;
		else if ("255".equals(value))
			return Onbekend;
		else if ("16".equals(value))
			return Stremming;
		else if ("24_14".equals(value))
			return Voetbalwedstrijd;
		else if ("18".equals(value))
			return Auto_in_spoor;
		else if ("23_1".equals(value))
			return Rioleringswerkzaamheden;
		else if ("23_2".equals(value))
			return Wegwerkzaamheden;
		else if ("23_3".equals(value))
			return Asfalteringswerkzaamheden;
		else if ("23_4".equals(value))
			return Bestratingswerkzaamheden;
		else if ("16".equals(value))
			return Route_versperd;
		else if ("24_15".equals(value))
			return Herdenking;
		else if ("24_16".equals(value))
			return Avondvierdaagse;
		else if ("4".equals(value))
			return Tekort_aan_personeel;
		else if ("5".equals(value))
			return Vakbondsacties;
		else if ("6".equals(value))
			return Stiptheidsacties;
		else if ("5_1".equals(value))
			return Mogelijke_staking;
		else if ("7".equals(value))
			return Defect_materieel;
		else if ("14".equals(value))
			return Defecte_brug;
		else if ("14_1".equals(value))
			return Defect_viaduct;
		else if ("8_4".equals(value))
			return Tekort_aan_materieel;
		else if ("6_2".equals(value))
			return Defecte_trein;
		else if ("12_1".equals(value))
			return Defecte_bovenleiding;
		else if ("8_1".equals(value))
			return Defect_spoor;
		else if ("5".equals(value))
			return Ontsporing;
		else if ("4".equals(value))
			return Seinstoring;
		else if ("8_10".equals(value))
			return Wisselstoring;
		else if ("12".equals(value))
			return Stroomstoring;
		else if ("8_11".equals(value))
			return Overwegstoring;
		else if ("4_1".equals(value))
			return Sein_en_wisselstoring;
		else if ("8_12".equals(value))
			return Storing_in_verkeersleidingssysteem;
		else if ("8_13".equals(value))
			return Gladde_sporen;
		else if ("11_2".equals(value))
			return Uitloop_werkzaamheden;
		else if ("11".equals(value))
			return Herstel_werkzaamheden;
		else if ("9".equals(value))
			return Herstel_werkzaamheden;
		else if ("11_2".equals(value))
			return Uitloop_herstel_werkzaamheden;
		else if ("9_1".equals(value))
			return Gladheid;
		else if ("9_2".equals(value))
			return IJsgang;
		else if ("3".equals(value))
			return Sneeuw;
		else if ("14".equals(value))
			return Wateroverlast;
		else if ("5".equals(value))
			return Storm;
		else if ("9_3".equals(value))
			return IJzel;
		else if ("255_1".equals(value))
			return Blikseminslag;
		else
			return null;
	}
}