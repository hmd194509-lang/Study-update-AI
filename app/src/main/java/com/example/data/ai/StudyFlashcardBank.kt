package com.example.data.ai

import com.example.data.model.FlashcardSubject
import com.example.data.model.StudyFlashcard

object StudyFlashcardBank {

    val defaultCards: List<StudyFlashcard> = listOf(
        // Physics
        StudyFlashcard(
            id = "fc_phy_1",
            subject = FlashcardSubject.PHYSICS,
            front = "প্রাসের সর্বোচ্চ উচ্চতা (H) এবং আনুভূমিক পাল্লা (R)-এর সূত্র কী?",
            back = "H = (v₀² sin²θ) / 2g\nR = (v₀² sin 2θ) / g\nসর্বোচ্চ পাল্লা θ = 45° হলে R_max = v₀² / g এবং তখন R_max = 4H।",
            admissionNote = "BUET Shortcut: θ ও (90°−θ) কোণে R সমান হয় এবং t₁·t₂ = 2R/g।"
        ),
        StudyFlashcard(
            id = "fc_phy_2",
            subject = FlashcardSubject.PHYSICS,
            front = "নদী পারাপারে ন্যূনতম সময় (t_min) ও সোজাসুজি (ন্যূনতম দূরত্ব) পার হওয়ার শর্ত কী?",
            back = "ন্যূনতম সময়: স্রোতের সাথে 90° কোণে রওনা দিতে হবে, t_min = d / v_b।\nন্যূনতম দূরত্ব (সোজাসুজি): cos α = −u / v_b (অর্থাৎ v_b > u হতে হবে)।",
            admissionNote = "DU 'Ka' MCQ: স্রোতের বেগ u ≥ নৌকার বেগ v_b হলে কখনোই সোজাসুজি অপর পাড়ে পৌঁছানো সম্ভব নয়।"
        ),
        StudyFlashcard(
            id = "fc_phy_3",
            subject = FlashcardSubject.PHYSICS,
            front = "সমোষ্ণ (Isothermal) বনাম রুদ্ধতাপীয় (Adiabatic) প্রক্রিয়ার মূল পার্থক্য কী?",
            back = "সমোষ্ণ: তাপমাত্রা স্থির (ΔT = 0, dU = 0), PV = const, dQ = dW।\nরুদ্ধতাপীয়: তাপের আদান-প্রদান শূন্য (dQ = 0), PV^γ = const, রুদ্ধতাপীয় লেখের ঢাল সমোষ্ণ লেখের γ গুণ খাড়া।",
            admissionNote = "Medical Trap: রুদ্ধতাপীয় প্রসারণে সিস্টেম কাজ করে অভ্যন্তরীণ শক্তি ব্যয় করে, তাই তাপমাত্রা কমে যায়।"
        ),
        StudyFlashcard(
            id = "fc_phy_4",
            subject = FlashcardSubject.PHYSICS,
            front = "ফটোইলেকট্রিক ক্রিয়ায় আইনস্টাইনের সমীকরণ ও নিবৃত্তি বিভব (Stopping Potential) কী?",
            back = "hf = φ₀ + K_max = hf₀ + eV_s\nনিবৃত্তি বিভব V_s শুধুমাত্র আপতিত আলোর কম্পাঙ্কের (frequency) ওপর নির্ভর করে, তীব্রতার (intensity) ওপর নয়।",
            admissionNote = "তীব্রতা বাড়ালে নিঃসৃত ইলেকট্রনের সংখ্যা (তড়িৎ প্রবাহ) বাড়ে, কিন্তু সর্বোচ্চ গতিশক্তি বাড়ে না।"
        ),
        StudyFlashcard(
            id = "fc_phy_5",
            subject = FlashcardSubject.PHYSICS,
            front = "ট্রানজিস্টরের কারেন্ট গেইন α এবং β-এর মধ্যে সম্পর্ক কী?",
            back = "α = I_C / I_E (সর্বদা < 1)\nβ = I_C / I_B\nসম্পর্ক: β = α / (1 − α) এবং α = β / (1 + β); I_E = I_B + I_C।",
            admissionNote = "যদি α = 0.98 হয়, তবে β = 0.98 / 0.02 = 49।"
        ),

        // Chemistry
        StudyFlashcard(
            id = "fc_chem_1",
            subject = FlashcardSubject.CHEMISTRY,
            front = "SN1 বনাম SN2 বিক্রিয়ার সক্রিয়তার ক্রম ও মেকানিজম কী?",
            back = "SN1 (২ ধাপে, কার্বোক্যাটায়ন মধ্যক): 3° > 2° > 1° (রেসিমিক মিশ্রণ তৈরি হয়)।\nSN2 (১ ধাপে, ব্যাকসাইড অ্যাটাক): CH₃X > 1° > 2° > 3° (ওয়াল্ডেন ইনভার্সন ঘটে)।",
            admissionNote = "পোলার প্রোটিক দ্রাবক SN1-কে এবং পোলার অ্যাপ্রোটিক দ্রাবক (DMSO, অ্যাসিটোন) SN2-কে ত্বরান্বিত করে।"
        ),
        StudyFlashcard(
            id = "fc_chem_2",
            subject = FlashcardSubject.CHEMISTRY,
            front = "অর্থো-প্যারা নির্দেশক বনাম মেটা নির্দেশক মূলক চেনার উপায় কী?",
            back = "অর্থো-প্যারা নির্দেশক (বলয় সক্রিয়কারী): −OH, −NH₂, −OR, −R, −X (হ্যালোজেন ব্যতিক্রম: সক্রিয়তা কমায় কিন্তু অর্থো-প্যারা)।\nমেটা নির্দেশক (বলয় নিষ্ক্রিয়কারী): −NO₂, −CHO, −COOH, −CN, −SO₃H।",
            admissionNote = "Medical & DU: যেসব মূলকে পাই (π) বা তড়িৎ ঋণাত্মক পরমাণু থাকে তারা সাধারণত মেটা নির্দেশক।"
        ),
        StudyFlashcard(
            id = "fc_chem_3",
            subject = FlashcardSubject.CHEMISTRY,
            front = "হেন্ডারসন-হ্যাসেলবাল্ক (বাফার pH) এবং Kp-Kc সম্পর্ক কী?",
            back = "অম্লীয় বাফার: pH = pKa + log([লবণ]/[অম্ল])\nক্ষারীয় বাফার: pOH = pKb + log([লবণ]/[ক্ষার])\nKp = Kc (RT)^Δn (যেখানে Δn = উৎপাদ গ্যাস মোল − বিক্রিয়ক গ্যাস মোল)।",
            admissionNote = "BUET Alert: Δn হিসাব করার সময় কঠিন (s) ও তরল (l) পদার্থ বাদ দিয়ে শুধু গ্যাস (g) গণনা করবে!"
        ),
        StudyFlashcard(
            id = "fc_chem_4",
            subject = FlashcardSubject.CHEMISTRY,
            front = "লুকাস বিকারক এবং আয়োডোফর্ম পরীক্ষা কোন যৌগগুলো দেয়?",
            back = "লুকাস বিকারক (গাঢ় HCl + অনার্দ্র ZnCl₂): 3° অ্যালকোহল তাৎক্ষণিক সাদা অধঃক্ষেপ, 2° (৫-১০ মিনিটে), 1° কক্ষ তাপমাত্রায় সাড়া দেয় না।\nআয়োডোফর্ম (I₂ + NaOH): CH₃CO− বা CH₃CH(OH)− মূলকযুক্ত যৌগ (ইথান্যাল, প্রোপানোন, ইথানল) হলুদ অধঃক্ষেপ দেয়।",
            admissionNote = "Medical Trap: ফরমালডিহাইড (HCHO) ও মিথানল (CH₃OH) আয়োডোফর্ম পরীক্ষা দেয় না।"
        ),
        StudyFlashcard(
            id = "fc_chem_5",
            subject = FlashcardSubject.CHEMISTRY,
            front = "ফ্যারাডের তড়িৎ বিশ্লেষণ সূত্র ও নার্নস্ট সমীকরণ কী?",
            back = "W = ZIt = (M · I · t) / (n · F), যেখানে 1 Faraday (F) = 96500 C/mol।\nনার্নস্ট (25°C): E_cell = E°_cell − (0.0591 / n) log Q।",
            admissionNote = "Al³⁺ থেকে ১ মোল Al মুক্ত করতে ৩ ফ্যারাডে (3F) বিদ্যুৎ প্রয়োজন।"
        ),

        // Biology
        StudyFlashcard(
            id = "fc_bio_1",
            subject = FlashcardSubject.BIOLOGY,
            front = "মায়োসিস-১ প্রোফেজ-১ এর কোন উপ-পর্যায়ে সিন্যাপসিস ও ক্রসিং ওভার ঘটে?",
            back = "• জাইগোটিন (Zygotene): হোমোলোগাস ক্রোমোজোমের জোড়া বাঁধা (সিন্যাপসিস / বাইভ্যালেন্ট)।\n• প্যাকাইটিন (Pachytene): ক্রসিং ওভার ও কায়াজমা সূচনা।\n• ডিপ্লোটিন (Diplotene): কায়াজমা স্পষ্ট দেখা যায় ও প্রান্তীয়করণ শুরু।",
            admissionNote = "Medical #1 MCQ: ক্রসিং ওভারের ফলে প্রকরণ (variation) ও নতুন বৈশিষ্ট্যের উদ্ভব ঘটে।"
        ),
        StudyFlashcard(
            id = "fc_bio_2",
            subject = FlashcardSubject.BIOLOGY,
            front = "মানব হৃদপিণ্ডের কার্ডিয়াক চক্র (0.8s) ও কপাটিকাগুলোর অবস্থান কী?",
            back = "কার্ডিয়াক চক্র = 0.8 সেকেন্ড:\n• অলিন্দের সিস্টোল: 0.1s | ডায়াস্টোল: 0.7s\n• নিলয়ের সিস্টোল: 0.3s | ডায়াস্টোল: 0.5s\nকপাটিকা: ডান অলিন্দ-নিলয়ে ট্রাইকাসপিড, বাম অলিন্দ-নিলয়ে বাইকাসপিড (মাইট্রাল)।",
            admissionNote = "SA নোডকে 'ন্যাচারাল পেসমেকার' বলা হয়; AV নোড সিগন্যাল ০.১ সেকেন্ড দেরি করায়।"
        ),
        StudyFlashcard(
            id = "fc_bio_3",
            subject = FlashcardSubject.BIOLOGY,
            front = "সালোকসংশ্লেষণের C3 ও C4 চক্রের প্রথম স্থায়ী পদার্থ ও এনজাইম কী?",
            back = "C3 (কেলভিন চক্র): CO₂ গ্রহীতা RuBP (5C), এনজাইম RuBisCO, প্রথম স্থায়ী যৌগ 3-PGA (3C)।\nC4 (হ্যাচ-স্ল্যাক): গ্রহীতা PEP (3C), প্রথম স্থায়ী যৌগ অক্সালোঅ্যাসিটিক অ্যাসিড OAA (4C), ক্র্যাঞ্জ অ্যানাটমি থাকে।",
            admissionNote = "C4 উদ্ভিদ উদাহরণ: আখ, ভুট্টা, মুথা ঘাস, অ্যামারান্থাস।"
        ),
        StudyFlashcard(
            id = "fc_bio_4",
            subject = FlashcardSubject.BIOLOGY,
            front = "DNA ও RNA-এর স্টার্ট কোডন, স্টপ কোডন এবং গ্লাইকোলাইসিসের ATP লাভ কত?",
            back = "স্টার্ট কোডন: AUG (মিথিওনিন নির্দেশ করে)।\nস্টপ কোডন: UAA, UAG, UGA।\nগ্লাইকোলাইসিস (সাইটোপ্লাজমে অক্সিজেন ছাড়াই): ১ গ্লুকোজ → ২ পাইরুভেট + ২ ATP (নিট) + ২ NADH।",
            admissionNote = "সবাত শ্বসনে ১ অণু গ্লুকোজ থেকে মোট ৩৬–৩৮ ATP তৈরি হয়।"
        ),

        // Higher Math
        StudyFlashcard(
            id = "fc_math_1",
            subject = FlashcardSubject.MATH,
            front = "L'Hôpital-এর নিয়ম এবং গুরুত্বপূর্ণ দুটি Limit মান কী?",
            back = "0/0 বা ∞/∞ আকারে থাকলে লব ও হরকে আলাদাভাবে অন্তরীকরণ (differentiate) করতে হয়।\n• lim(x→0) (sin x)/x = 1\n• lim(x→0) (1 + x)^(1/x) = e\n• ∫₀^(π/2) sinⁿx dx (Walli's formula)।",
            admissionNote = "DU & BUET MCQ-তে সিরিজ সম্প্রসারণ বা L'Hôpital ব্যবহার করলে ১০ সেকেন্ডে উত্তর মেলে।"
        ),
        StudyFlashcard(
            id = "fc_math_2",
            subject = FlashcardSubject.MATH,
            front = "দুটি সরলরেখা লম্ব ও সমান্তরাল হওয়ার শর্ত এবং বিন্দু থেকে রেখার দূরত্ব কী?",
            back = "লম্ব হলে: m₁ · m₂ = −1 (অথবা a₁a₂ + b₁b₂ = 0)।\nসমান্তরাল হলে: m₁ = m₂।\n(x₁, y₁) থেকে ax + by + c = 0 রেখার লম্ব দূরত্ব d = |ax₁ + by₁ + c| / √(a² + b²)।",
            admissionNote = "ax + by + c₁ = 0 এবং ax + by + c₂ = 0 সমান্তরাল রেখাদ্বয়ের দূরত্ব = |c₁ − c₂| / √(a² + b²)। "
        ),

        // General / Study Mastery
        StudyFlashcard(
            id = "fc_gen_1",
            subject = FlashcardSubject.GENERAL,
            front = "Active Recall এবং Spaced Repetition কীভাবে পরীক্ষার নম্বর বাড়ায়?",
            back = "শুধু বই বারবার পড়ার চেয়ে নিজেকে প্রশ্ন করা (Active Recall) এবং ১ দিন, ৩ দিন, ৭ দিন পরপর রিভিশন দেওয়া (Spaced Repetition) দীর্ঘমেয়াদী স্মৃতিতে সূত্র ও তথ্য গেঁথে দেয়।",
            admissionNote = "প্রতিদিন ১৫ মিনিট Flashcards প্র্যাকটিস করলে HSC ও Admission পরীক্ষার ভুলে যাওয়ার প্রবণতা কমে।"
        ),
        StudyFlashcard(
            id = "fc_gen_2",
            subject = FlashcardSubject.GENERAL,
            front = "Pomodoro + App Time Limit কৌশল কীভাবে ফোকাস বাড়ায়?",
            back = "২৫–৪৫ মিনিট একটানা গভীর মনোযোগে পড়াশোনা করে ৫ মিনিট বিরতি নাও। ডিস্ট্রাক্টিং সোশ্যাল অ্যাপগুলোতে দৈনিক টাইম লিমিট সেট রাখো যাতে পড়ার সময় নষ্ট না হয়।",
            admissionNote = "Study Update AI-এর App Time Limit & Unlock সিস্টেমে জরুরি কারণ ছাড়া এক্সট্রা সময় অনুমোদন হয় না।"
        )
    )
}
