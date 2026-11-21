package com.example.data.ai

import com.example.data.model.HscSubject
import com.example.data.model.HscTopicCard

object HscKnowledgeBank {

    val topics: List<HscTopicCard> = listOf(
        // HSC & Admission Physics
        HscTopicCard(
            id = "phy_vector_projectile",
            subject = HscSubject.PHYSICS,
            title = "Vectors, Projectile & Circular Motion",
            bnTitle = "ভেক্টর, প্রাস ও বৃত্তাকার গতি",
            levelBadge = "HSC 1st Paper • BUET / DU",
            keyFormulasOrFacts = listOf(
                "Projectile Range: R = (v₀² sin 2θ) / g, Max Height: H = (v₀² sin²θ) / 2g",
                "Trajectory: y = x tanθ − (g x²) / (2 v₀² cos²θ)",
                "Centripetal Force: F = mv²/r = mω²r, Banking: tanθ = v² / (rg)",
                "River-Boat: Minimum time t_min = d / v_b; Shortest path cosα = −u / v_b"
            ),
            admissionTip = "BUET & CKRUET trick: For equal range at angles θ and (90°−θ), t₁·t₂ = 2R/g and H₁ + H₂ = v₀²/(2g).",
            explainPrompt = "HSC ও BUET/DU অ্যাডমিশন লেভেলের জন্য ভেক্টর, নদী-নৌকা ও প্রাসের গতি (Projectile Motion) সম্পূর্ণ সূত্র, শর্টকাট এবং একটি গাইডলাইন CQ + MCQ সহ বুঝিয়ে দাও।",
            diagramPrompt = "Clean educational scientific diagram of Projectile Motion on white background, showing initial velocity v0, launch angle theta, parabolic trajectory, maximum height H, horizontal range R, velocity components vx and vy, with crisp physics labels."
        ),
        HscTopicCard(
            id = "phy_thermo_optics",
            subject = HscSubject.PHYSICS,
            title = "Thermodynamics & Physical Optics",
            bnTitle = "তাপগতিবিদ্যা ও ভৌত আলোকবিজ্ঞান",
            levelBadge = "HSC 2nd Paper • Medical / Engg",
            keyFormulasOrFacts = listOf(
                "1st Law: dQ = dU + dW; Adiabatic: PV^γ = const, TV^(γ−1) = const",
                "Carnot Efficiency: η = 1 − T₂/T₁; Entropy: ΔS = ∫ dQ/T",
                "Young's Double Slit: Fringe width β = λD / a; Constructive Δx = nλ",
                "Diffraction Grating: d sinθ = nλ (n_max ≤ d/λ)"
            ),
            admissionTip = "Medical & DU 'Ka' note: In isothermal expansion dU = 0 (so dQ = dW), while in adiabatic expansion dQ = 0 (so temperature drops).",
            explainPrompt = "তাপগতিবিদ্যার প্রথম ও দ্বিতীয় সূত্র, কার্নো ইঞ্জিন, এনট্রপি এবং ইয়ং-এর দ্বি-চিড় পরীক্ষার সূত্র ও অ্যাডমিশন শর্টকাট বাংলায় উদাহরণসহ বুঝিয়ে দাও।",
            diagramPrompt = "Clean labeled physics textbook diagram of Young's Double Slit Interference experiment on white background showing coherent light source, slits S1 and S2, distance D, screen interference fringes bright and dark bands, and path difference delta x."
        ),
        HscTopicCard(
            id = "phy_modern_semiconductor",
            subject = HscSubject.PHYSICS,
            title = "Modern Physics & Semiconductors",
            bnTitle = "আধুনিক পদার্থবিজ্ঞান ও সেমিকন্ডাক্টর",
            levelBadge = "HSC 2nd Paper • BUET / Medical",
            keyFormulasOrFacts = listOf(
                "Photoelectric Eq: hf = φ₀ + K_max = hf₀ + eV_s",
                "de Broglie Wavelength: λ = h / p = h / √(2mK)",
                "Radioactive Decay: N = N₀ e^(−λt), Half-life T₁/₂ = ln(2)/λ = 0.693/λ",
                "Transistor Gain: α = I_C/I_E, β = I_C/I_B, β = α/(1−α)"
            ),
            admissionTip = "High-yield MCQ: Stopping potential V_s depends ONLY on frequency, whereas photoelectric current depends on intensity.",
            explainPrompt = "আধুনিক পদার্থবিজ্ঞান (ফটোইলেকট্রিক ক্রিয়া, তেজস্ক্রিয় ক্ষয়, অর্ধায়ু) এবং ডায়োড-ট্রানজিস্টরের (p-n junction, α ও β) সম্পূর্ণ HSC ও অ্যাডমিশন নোট দাও।",
            diagramPrompt = "Clean labeled scientific diagram of a p-n junction diode and NPN transistor circuit symbol with depletion region, majority carriers electrons and holes, emitter, base, collector clearly marked on white background."
        ),

        // HSC & Admission Chemistry
        HscTopicCard(
            id = "chem_organic_mechanisms",
            subject = HscSubject.CHEMISTRY,
            title = "Organic Reactions: SN1/SN2, Benzene & Aldol",
            bnTitle = "জৈব রসায়ন: SN1/SN2, বেনজিন ও অ্যালডল",
            levelBadge = "HSC 2nd Paper • BUET / Medical / DU",
            keyFormulasOrFacts = listOf(
                "SN1 Reactivity: 3° > 2° > 1° (carbocation intermediate, racemization)",
                "SN2 Reactivity: CH₃X > 1° > 2° > 3° (backside attack, Walden inversion)",
                "Ortho-Para Directors: −OH, −NH₂, −OCH₃, −R, −X; Meta: −NO₂, −CHO, −COOH, −CN",
                "Tests: Lucas (1°/2°/3° alcohol), Tollens & Fehling (Aldehyde), Iodoform (CH₃CO−)"
            ),
            admissionTip = "Medical & DU Favorite: Phenol + Br₂(aq) gives white precipitate of 2,4,6-tribromophenol; Chloroform + O₂ + light forms poisonous phosgene (COCl₂).",
            explainPrompt = "HSC ও অ্যাডমিশন লেভেলের জৈব রসায়ন: SN1 বনাম SN2 বিক্রিয়া, মার্কনিকভ/খারাশ নীতি, বেনজিনের ইলেকট্রোফিলিক প্রতিস্থাপন ও লুকাস/টলেন/আয়োডোফর্ম শনাক্তকরণ বিক্রিয়াগুলো বিস্তারিত বুঝিয়ে দাও।",
            diagramPrompt = "Clean chemistry textbook diagram showing Benzene resonance hybrid structure and SN2 backside nucleophilic attack mechanism with partial bonds and orbital geometry on clean white background."
        ),
        HscTopicCard(
            id = "chem_chemical_change_electro",
            subject = HscSubject.CHEMISTRY,
            title = "Chemical Equilibrium, pH & Electrochemistry",
            bnTitle = "রাসায়নিক সাম্যাবস্থা, pH ও তড়িৎ রসায়ন",
            levelBadge = "HSC 1st & 2nd Paper • Engg / Medical",
            keyFormulasOrFacts = listOf(
                "Kp = Kc (RT)^Δn; Ostwald Dilution: α = √(Ka / C), [H⁺] = √(Ka·C)",
                "Buffer (Henderson): pH = pKa + log([Salt]/[Acid])",
                "Faraday's 1st Law: W = ZIt = (M·I·t) / (n·F), F = 96500 C/mol",
                "Nernst Eq (25°C): E_cell = E°_cell − (0.0591 / n) log Q"
            ),
            admissionTip = "BUET math check: Always calculate Δn = n_products(g) − n_reactants(g) counting ONLY gaseous species!",
            explainPrompt = "রাসায়নিক পরিবর্তন (Kp, Kc, বাফার দ্রবণ, pH অংক) এবং তড়িৎ রসায়ন (ফ্যারাডের সূত্র ও নার্নস্ট সমীকরণ) HSC + BUET মানের উদাহরণ অংকসহ বুঝিয়ে দাও।",
            diagramPrompt = "Clean labeled electrochemistry textbook diagram of a Daniell Galvanic Cell (Zn/ZnSO4 || CuSO4/Cu) showing anode, cathode, salt bridge, electron flow direction, and half-cell reactions on white background."
        ),
        HscTopicCard(
            id = "chem_qualitative_orbitals",
            subject = HscSubject.CHEMISTRY,
            title = "Quantum Numbers, Hybridization & Solubility",
            bnTitle = "কোয়ান্টাম সংখ্যা, সংকরায়ন ও দ্রাব্যতা গুণফল",
            levelBadge = "HSC 1st Paper • Medical / DU",
            keyFormulasOrFacts = listOf(
                "Quantum Numbers: n (shell), l (0..n−1), m (−l..+l), s (±½); Max e⁻ = 2n²",
                "Hybridization (H = ½[V + M − C + A]): sp (180°), sp² (120°), sp³ (109.5°)",
                "VSEPR Lone Pair Repulsion: NH₃ (107°), H₂O (104.5°)",
                "Solubility Product: For AₓBᵧ, Ksp = xˣ · yʸ · S^(x+y); Precipitates if Kip > Ksp"
            ),
            admissionTip = "Quick shortcut: Cu (29) and Cr (24) violate Aufbau rule for half-filled/full-filled d-orbital stability; remember Fajan's rules for covalent character!",
            explainPrompt = "গুণগত রসায়ন অধ্যায়ের কোয়ান্টাম সংখ্যা, আউফবাউ/হুন্ড/পাউলি নীতি, সংকরায়ন নির্ণয়ের শর্টকাট ও দ্রাব্যতা গুণফল (Ksp বনাম Kip) অংকসহ বাংলায় বুঝিয়ে দাও।",
            diagramPrompt = "Clean labeled chemistry diagram illustrating sp3, sp2, and sp orbital hybridization geometries (tetrahedral 109.5 deg, trigonal planar 120 deg, linear 180 deg) with bond angles on crisp white background."
        ),

        // HSC & Admission Biology
        HscTopicCard(
            id = "bio_cell_genetics",
            subject = HscSubject.BIOLOGY,
            title = "Cell Division, DNA Replication & Genetics",
            bnTitle = "কোষ বিভাজন, DNA অনুলিপন ও জিনতত্ত্ব",
            levelBadge = "HSC Botany & Zoology • Medical",
            keyFormulasOrFacts = listOf(
                "Mitosis: Metaphase (equatorial plate), Anaphase (V, L, J, I centromere shapes)",
                "Meiosis I Prophase I: Leptotene → Zygotene (synapsis) → Pachytene (crossing over) → Diplotene (chiasmata)",
                "Central Dogma: DNA —(Replication)→ DNA —(Transcription)→ mRNA —(Translation)→ Protein",
                "Mendelian Ratios: Monohybrid 3:1, Dihybrid 9:3:3:1; Complementary 9:7; Epistasis 13:3 / 9:3:4"
            ),
            admissionTip = "Medical Admission #1 High-Yield: Crossing over occurs in Pachytene; Start codon is AUG (Methionine); Stop codons are UAA, UAG, UGA.",
            explainPrompt = "HSC ও মেডিকেল অ্যাডমিশনের জন্য কোষ বিভাজন (মাইটোসিস ও মায়োসিস প্রোফেজ-১), DNA-RNA ট্রান্সক্রিপশন-ট্রান্সলেশন এবং মেন্ডেলের সূত্রের ব্যতিক্রমসমূহের অনুপাত বাংলায় গুছিয়ে বুঝিয়ে দাও।",
            diagramPrompt = "Clean labeled biology textbook diagram of DNA double helix replication fork showing leading strand, lagging strand, Okazaki fragments, DNA polymerase, helicase, and base pairs A-T and G-C on white background."
        ),
        HscTopicCard(
            id = "bio_human_heart_nephron",
            subject = HscSubject.BIOLOGY,
            title = "Human Heart, Cardiac Cycle & Nephron",
            bnTitle = "মানব হৃদপিণ্ড, কার্ডিয়াক চক্র ও নেফ্রন",
            levelBadge = "HSC Zoology • Medical Admission",
            keyFormulasOrFacts = listOf(
                "Cardiac Cycle: 0.8s (Atrial systole 0.1s, Ventricular systole 0.3s, Joint diastole 0.4s)",
                "Pacemaker: SA Node → AV Node → Bundle of His → Purkinje fibers",
                "Valves: Tricuspid (right AV), Bicuspid/Mitral (left AV), Semilunar (aortic & pulmonary)",
                "Nephron Filtration: Bowman's capsule (ultrafiltration), Loop of Henle (counter-current), ADH regulates DCT/Collecting duct"
            ),
            admissionTip = "Medical Admission Trap: Pulmonary artery carries DEOXYGENATED blood; Pulmonary vein carries OXYGENATED blood; Umbilical vein carries oxygenated blood!",
            explainPrompt = "মানব দেহের রক্ত সংবহন (হৃদপিণ্ডের কপাটিকা, পেসমেকার SA-AV নোড, কার্ডিয়াক চক্র) ও বৃক্কের নেফ্রনের মূত্র তৈরির প্রক্রিয়া HSC CQ এবং মেডিকেল MCQ নোটসহ বুঝিয়ে দাও।",
            diagramPrompt = "Clean labeled medical textbook cross-section diagram of the Human Heart on white background showing Right Atrium, Left Atrium, Right Ventricle, Left Ventricle, Aorta, Vena Cava, Pulmonary Artery, Tricuspid and Mitral valves with blood flow arrows."
        ),
        HscTopicCard(
            id = "bio_photosynthesis_respiration",
            subject = HscSubject.BIOLOGY,
            title = "Photosynthesis (C3/C4) & Cellular Respiration",
            bnTitle = "সালোকসংশ্লেষণ (C3/C4) ও কোষীয় শ্বসন",
            levelBadge = "HSC Botany • Medical / Agri / DU",
            keyFormulasOrFacts = listOf(
                "C3 (Calvin Cycle): Primary CO₂ acceptor RuBP (5C), enzyme RuBisCO, 1st stable compound 3-PGA (3C)",
                "C4 (Hatch-Slack): Mesophyll PEP (3C) → OAA (4C), Kranz anatomy in bundle sheath (Sugarcane, Maize)",
                "Glycolysis (Cytoplasm): 1 Glucose → 2 Pyruvate + 2 ATP (net) + 2 NADH",
                "Krebs Cycle (Mitochondrial matrix): Net aerobic yield ≈ 36–38 ATP per glucose"
            ),
            admissionTip = "Medical MCQ: Glycolysis is common to both aerobic and anaerobic respiration and requires NO oxygen; ETS occurs on inner mitochondrial membrane (cristae).",
            explainPrompt = "উদ্ভিদ শারীরতত্ত্ব: সালোকসংশ্লেষণের আলোক ও অন্ধকার পর্যায় (C3 ও C4 চক্রের পার্থক্য) এবং শ্বসনের গ্লাইকোলাইসিস ও ক্রেবস চক্রের ATP হিসাব সম্পূর্ণ চার্টসহ বাংলায় বুঝিয়ে দাও।",
            diagramPrompt = "Clean labeled biology textbook diagram of a Chloroplast and Mitochondrion side by side showing thylakoid grana, stroma (Calvin cycle), cristae, and matrix (Krebs cycle) on white background."
        )
    )
}
