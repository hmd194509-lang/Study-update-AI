package com.example.data.ai

import com.example.data.model.AdmissionCategory
import com.example.data.model.AdmissionUniversityInfo

object AdmissionDirectoryBank {

    val entries: List<AdmissionUniversityInfo> = listOf(
        // ==================== MEDICAL & DENTAL ====================
        AdmissionUniversityInfo(
            id = "adm_medical_mbbs",
            name = "Govt & Private Medical (MBBS - DGME)",
            bnName = "সরকারি ও বেসরকারি মেডিকেল কলেজ (MBBS)",
            category = AdmissionCategory.MEDICAL,
            circularSchedule = "সম্ভাব্য সার্কুলার: HSC ফলাফল প্রকাশের ৩–৪ সপ্তাহ পর (ডিসেম্বর – জানুয়ারি)। আবেদন চলে প্রায় ২–৩ সপ্তাহ।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – ফেব্রুয়ারি (একযোগে ১০০ নম্বরের লিখিত MCQ)।",
            eligibilityAndMarks = "• SSC + HSC মোট GPA ≥ 9.0 (উভয় পরীক্ষায় আলাদা ≥ 4.0 এবং Biology-তে ন্যূনতম GPA 4.0)।\n• ১০০ MCQ (Biology 30, Chemistry 25, Physics 20, English 15, GK 10) + GPA ১০০ = মোট ২০০ নম্বর। নেগেটিভ: ০.২৫।",
            registrationUrl = "https://dgme.teletalk.com.bd",
            noticeUrl = "https://dgme.gov.bd",
            aiPrepPrompt = "মেডিকেল (MBBS) ভর্তি পরীক্ষার Biology, Chemistry ও Physics-এর কোন অধ্যায় থেকে বেশি প্রশ্ন আসে এবং নেগেটিভ মার্কিং কমানোর কৌশল দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_dental_bds",
            name = "Dental College Admission (BDS)",
            bnName = "সরকারি ও বেসরকারি ডেন্টাল কলেজ ও ইউনিট (BDS)",
            category = AdmissionCategory.MEDICAL,
            circularSchedule = "সম্ভাব্য সার্কুলার: MBBS পরীক্ষার ফলাফলের পরপরই (ফেব্রুয়ারি – মার্চ)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – এপ্রিল।",
            eligibilityAndMarks = "• SSC + HSC মোট GPA ≥ 9.0 ও Biology-তে GPA 4.0+।\n• MBBS-এর অনুরূপ ১০০ নম্বরের MCQ (Biology 30, Chemistry 25, Physics 20, English 15, GK 10)।",
            registrationUrl = "https://dgme.teletalk.com.bd",
            noticeUrl = "https://dgme.gov.bd",
            aiPrepPrompt = "ডেন্টাল (BDS) ভর্তি পরীক্ষার প্রস্তুতি, ঢাকা ডেন্টাল কলেজ কাট-মার্কস এবং রিভিশন প্ল্যান তৈরি করে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_afmc",
            name = "AFMC & Army Medical Colleges",
            bnName = "আর্মড ফোর্সেস মেডিকেল কলেজ (AFMC) ও আর্মি মেডিকেল",
            category = AdmissionCategory.MEDICAL,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – ফেব্রুয়ারি।",
            examSchedule = "সম্ভাব্য পরীক্ষা: ফেব্রুয়ারি – মার্চ (লিখিত, ভাইভা ও মেডিকেল ফিটনেস টেস্ট)।",
            eligibilityAndMarks = "• অবিবাহিত বাংলাদেশি নাগরিক; SSC + HSC মোট GPA ≥ 9.0 (Biology 4.0+)।\n• ১০০ MCQ পরীক্ষা (Physics, Chemistry, Biology, English, GK) + সাক্ষাৎকার।",
            registrationUrl = "https://afmc.teletalk.com.bd",
            noticeUrl = "https://afmc.edu.bd",
            aiPrepPrompt = "AFMC এবং আর্মি মেডিকেল কলেজ ভর্তির লিখিত, স্বাস্থ্য পরীক্ষা ও ভাইভা প্রস্তুতির পূর্ণ গাইড দাও।"
        ),

        // ==================== ENGINEERING ====================
        AdmissionUniversityInfo(
            id = "adm_buet",
            name = "BUET (Bangladesh Univ. of Engineering & Tech)",
            bnName = "বাংলাদেশ প্রকৌশল বিশ্ববিদ্যালয় (বুয়েট)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: নভেম্বর – জানুয়ারি (অনলাইন আবেদন + প্রিলিমিনারি শর্টলিস্ট)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: প্রিলিমিনারি (জানুয়ারি/ফেব্রুয়ারি) ও চূড়ান্ত লিখিত পরীক্ষা (ফেব্রুয়ারি/মার্চ)।",
            eligibilityAndMarks = "• Higher Math, Physics ও Chemistry-তে উচ্চ নম্বর ও GPA 5.0 শর্ত।\n• প্রিলিমিনারি ১০০ MCQ থেকে নির্বাচিত প্রার্থীরা ৪০০ নম্বরের লিখিত পরীক্ষায় অংশ নেয়।",
            registrationUrl = "https://ugadmission.buet.ac.bd",
            noticeUrl = "https://www.buet.ac.bd",
            aiPrepPrompt = "BUET প্রিলিমিনারি ও লিখিত পরীক্ষার Physics, Chemistry ও Math-এর গুরুত্বপূর্ণ টপিক এবং সময় বণ্টন কৌশল দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_cuet",
            name = "CUET (Chittagong Univ. of Engineering & Tech)",
            bnName = "চট্টগ্রাম প্রকৌশল ও প্রযুক্তি বিশ্ববিদ্যালয় (চুয়েট)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – জানুয়ারি (CKRUET গুচ্ছ বা নিজস্ব সার্কুলার)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – মার্চ (গ্রুপ 'ক' ইঞ্জিনিয়ারিং ও 'খ' আর্কিটেকচার)।",
            eligibilityAndMarks = "• Physics, Chemistry, Higher Math ও English মিলে শর্টলিস্ট ও ৫০০ নম্বরের পরীক্ষা (Math 150, Physics 150, Chem 150, English 50)।",
            registrationUrl = "https://admissioncuet.ac.bd",
            noticeUrl = "https://www.cuet.ac.bd",
            aiPrepPrompt = "CUET ভর্তি পরীক্ষার প্রস্তুতি, ক্যালকুলেটর ব্যবহারের নিয়ম ও গুরুত্বপূর্ণ অধ্যায়গুলো সাজিয়ে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_kuet",
            name = "KUET (Khulna Univ. of Engineering & Tech)",
            bnName = "খুলনা প্রকৌশল ও প্রযুক্তি বিশ্ববিদ্যালয় (কুয়েট)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – জানুয়ারি।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – মার্চ।",
            eligibilityAndMarks = "• Math, Physics, Chemistry ও English-এ ৫০০ নম্বরের পরীক্ষা (প্রতি ভুল উত্তরে ২৫% নেগেটিভ মার্কিং থাকে)।",
            registrationUrl = "https://admission.kuet.ac.bd",
            noticeUrl = "https://www.kuet.ac.bd",
            aiPrepPrompt = "KUET ভর্তি পরীক্ষার ম্যাথ, ফিজিক্স ও কেমিস্ট্রি প্রশ্ন প্যাটার্ন ও শর্টকাট আলোচনা করো।"
        ),
        AdmissionUniversityInfo(
            id = "adm_ruet",
            name = "RUET (Rajshahi Univ. of Engineering & Tech)",
            bnName = "রাজশাহী প্রকৌশল ও প্রযুক্তি বিশ্ববিদ্যালয় (রুয়েট)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – জানুয়ারি।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – মার্চ (প্রিলিমিনারি / লিখিত বা সমন্বিত)।",
            eligibilityAndMarks = "• Math, Physics, Chemistry ও English-এ HSC-র উচ্চ জিপিএ ও নম্বর অনুযায়ী প্রিলি/মূল পরীক্ষা।",
            registrationUrl = "https://admission.ruet.ac.bd",
            noticeUrl = "https://www.ruet.ac.bd",
            aiPrepPrompt = "RUET ভর্তি পরীক্ষার প্রিলিমিনারি ও লিখিত অংশের জন্য উচ্চ-ফলনশীল অধ্যায়গুলো বলে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_iut",
            name = "IUT (Islamic University of Technology)",
            bnName = "ইসলামিক ইউনিভার্সিটি অব টেকনোলজি (আইইউটি - গাজীপুর)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – ফেব্রুয়ারি (OIC আন্তর্জাতিক বিশ্ববিদ্যালয়)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – এপ্রিল (সম্পূর্ণ ইংরেজি মাধ্যমে ১০০ MCQ)।",
            eligibilityAndMarks = "• English Medium প্রশ্নপত্র: Math, Physics, Chemistry ও English (১০০ নম্বর, ২ ঘণ্টা; O-Level/A-Level ও HSC প্রার্থীরা যোগ্য)।",
            registrationUrl = "https://admission.iutoic-dhaka.edu",
            noticeUrl = "https://www.iutoic-dhaka.edu",
            aiPrepPrompt = "IUT ভর্তি পরীক্ষার ইংরেজি ভার্সনের Physics, Math ও Chemistry প্রশ্ন অনুশীলনের সেরা কৌশল দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_butex",
            name = "BUTEX (Bangladesh Univ. of Textiles)",
            bnName = "বাংলাদেশ টেক্সটাইল বিশ্ববিদ্যালয় (বুটেক্স)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – জানুয়ারি।",
            examSchedule = "সম্ভাব্য পরীক্ষা: ফেব্রুয়ারি – মার্চ (লিখিত ২০০ নম্বর)।",
            eligibilityAndMarks = "• SSC/HSC-তে ভালো জিপিএ + Physics (60), Chemistry (60), Higher Math (60) ও English (20) — লিখিত পরীক্ষা (২০০ নম্বর)।",
            registrationUrl = "https://admission.butex.edu.bd",
            noticeUrl = "https://www.butex.edu.bd",
            aiPrepPrompt = "BUTEX লিখিত ভর্তি পরীক্ষার Chemistry, Physics ও Math প্রশ্নপদ্ধতি বুঝিয়ে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_mist",
            name = "MIST (Military Institute of Science & Tech)",
            bnName = "মিলিটারি ইনস্টিটিউট অব সায়েন্স অ্যান্ড টেকনোলজি (মিস্ট)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – জানুয়ারি।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – ফেব্রুয়ারি (সবার আগে অনুষ্ঠিত ইঞ্জিনিয়ারিং পরীক্ষাগুলোর একটি)।",
            eligibilityAndMarks = "• Unit-A (Engineering): Math, Physics, Chemistry & English লিখিত পরীক্ষা (৩ ঘণ্টা)।\n• Unit-B (Architecture): অঙ্কনসহ পরীক্ষা।",
            registrationUrl = "https://admission.mist.ac.bd",
            noticeUrl = "https://mist.ac.bd",
            aiPrepPrompt = "MIST ইঞ্জিনিয়ারিং ভর্তি পরীক্ষার মানবণ্টন, দ্রুত ক্যালকুলেশন ও প্রস্তুতি রোডম্যাপ দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_duet",
            name = "DUET (Dhaka Univ. of Engineering & Tech)",
            bnName = "ঢাকা প্রকৌশল ও প্রযুক্তি বিশ্ববিদ্যালয় (ডুয়েট - গাজীপুর)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: এপ্রিল – মে (ডিপ্লোমা ইঞ্জিনিয়ারিং উত্তীর্ণদের জন্য)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মে – জুন।",
            eligibilityAndMarks = "• ডিপ্লোমা-ইন-ইঞ্জিনিয়ারিং ন্যূনতম CGPA 3.00+\n• ১ম পত্র (নন-ডিপার্টমেন্টাল: Physics, Chem, Math, English) ও ২য় পত্র (ডিপার্টমেন্টাল)।",
            registrationUrl = "https://admission.duet.ac.bd",
            noticeUrl = "https://www.duet.ac.bd",
            aiPrepPrompt = "DUET ভর্তি পরীক্ষার Non-Departmental ও Departmental সিলেবাস এবং কমন টপিকগুলো বুঝিয়ে দাও।"
        ),

        // ==================== PUBLIC GENERAL & AUTONOMOUS ====================
        AdmissionUniversityInfo(
            id = "adm_du",
            name = "Dhaka University (DU - Ka, Kha, Ga, Cha)",
            bnName = "ঢাকা বিশ্ববিদ্যালয় (ঢাবি - ক, খ, গ, চ ইউনিট)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: নভেম্বর – ডিসেম্বর (আবেদন ৩–৪ সপ্তাহ)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – ফেব্রুয়ারি (২য় বার পরীক্ষার সুযোগ নেই)।",
            eligibilityAndMarks = "• বিজ্ঞান ('ক' ইউনিট): SSC+HSC GPA ≥ 8.0। ১২০ নম্বর: MCQ ৬০ + লিখিত ৪০ + GPA ২০।\n• 'খ' (কলা/মানবিক) ও 'গ' (ব্যবসায়) ইউনিটেও MCQ+লিখিত পরীক্ষা হয়।",
            registrationUrl = "https://admission.eis.du.ac.bd",
            noticeUrl = "https://www.du.ac.bd",
            aiPrepPrompt = "ঢাকা বিশ্ববিদ্যালয় ক, খ ও গ ইউনিটের MCQ + লিখিত ৪০ নম্বরের পূর্ণ প্রস্তুতি প্ল্যান সাজিয়ে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_du_7_colleges",
            name = "Dhaka Affiliated 7 Colleges",
            bnName = "ঢাকা অধিভুক্ত সরকারি সাত কলেজ (ঢাকা কলেজ, ইডেন, তিতুমীর প্রভৃতি)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: ফেব্রুয়ারি – মার্চ।",
            examSchedule = "সম্ভাব্য পরীক্ষা: এপ্রিল – মে।",
            eligibilityAndMarks = "• বিজ্ঞান, কলা ও ব্যবসায় আলাদা ইউনিটে ১০০ MCQ + GPA ২০ = মোট ১২০ নম্বর।",
            registrationUrl = "https://collegeadmission.eis.du.ac.bd",
            noticeUrl = "https://collegeadmission.eis.du.ac.bd",
            aiPrepPrompt = "সরকারি ৭ কলেজ ভর্তি পরীক্ষার বিজ্ঞান/কলা/ব্যবসায় ইউনিটের প্রশ্ন কাঠামো ও প্রস্তুতি টিপস দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_ju",
            name = "Jahangirnagar University (JU)",
            bnName = "জাহাঙ্গীরনগর বিশ্ববিদ্যালয় (জাবি - A, B, C, D, E ইউনিট)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – ফেব্রুয়ারি।",
            examSchedule = "সম্ভাব্য পরীক্ষা: ফেব্রুয়ারি – মার্চ (শিফটভিত্তিক MCQ পরীক্ষা)।",
            eligibilityAndMarks = "• ইউনিট A (গাণিতিক ও পদার্থ), ইউনিট D (জীববিজ্ঞান - ফার্মেসি/জেনেটিক), ইউনিট E (IBA-JU)।\n• ৮০ নম্বরের MCQ (৫৫ মিনিট) + GPA ২০ = ১০০ নম্বর।",
            registrationUrl = "https://juniv-admission.org",
            noticeUrl = "https://www.juniv.edu",
            aiPrepPrompt = "জাহাঙ্গীরনগর বিশ্ববিদ্যালয়ের Unit-A ও Unit-D (ফার্মেসি/জীববিজ্ঞান) পরীক্ষার শর্টকাট ও IQ প্রস্তুতি দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_ru",
            name = "University of Rajshahi (RU)",
            bnName = "রাজশাহী বিশ্ববিদ্যালয় (রাবি - ইউনিট A, B, C)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – জানুয়ারি।",
            examSchedule = "সম্ভাব্য পরীক্ষা: ফেব্রুয়ারি – মার্চ (১০০ নম্বরের MCQ, ৬০ মিনিট)।",
            eligibilityAndMarks = "• Unit C (বিজ্ঞান): Physics, Chemistry + Math/Bio/ICT মিলিয়ে ৮০ MCQ = ১০০ নম্বর (জিপিএ মার্কস নেই)। বিভাগীয় শহরেও কেন্দ্র থাকতে পারে।",
            registrationUrl = "https://admission.ru.ac.bd",
            noticeUrl = "https://www.ru.ac.bd",
            aiPrepPrompt = "রাজশাহী বিশ্ববিদ্যালয় (RU Unit-C) ভর্তি পরীক্ষার ৮০টি MCQ ১ ঘণ্টায় সমাধানের সেরা কৌশল দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_cu",
            name = "University of Chittagong (CU)",
            bnName = "চট্টগ্রাম বিশ্ববিদ্যালয় (চবি - A, B, C, D ইউনিট)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – ফেব্রুয়ারি।",
            examSchedule = "সম্ভাব্য পরীক্ষা: ফেব্রুয়ারি – মার্চ।",
            eligibilityAndMarks = "• Unit A (বিজ্ঞান): ১০০ MCQ (Bangla, English, Physics, Chem, Math/Bio) + GPA ২০ = ১২০ নম্বর।",
            registrationUrl = "https://admission.cu.ac.bd",
            noticeUrl = "https://www.cu.ac.bd",
            aiPrepPrompt = "চট্টগ্রাম বিশ্ববিদ্যালয় Unit-A পরীক্ষার প্রশ্ন প্যাটার্ন, ইংরেজি অংশ ও বিজ্ঞান বিষয়ের প্রস্তুতি গাইড দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_bup",
            name = "BUP (Bangladesh Univ. of Professionals)",
            bnName = "বাংলাদেশ ইউনিভার্সিটি অব প্রফেশনালস (বিইউপি)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: নভেম্বর – ডিসেম্বর।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – ফেব্রুয়ারি (FST, FBS, FASS, FSSS ফ্যাকাল্টি)।",
            eligibilityAndMarks = "• প্রশ্ন সম্পূর্ণ ইংরেজিতে। FST (Engineering/ICT/CSE): Physics, Chem, Math/Bio, English।\n• FBS/FASS: GK, Analytical ও English গুরুত্বপূর্ণ।",
            registrationUrl = "https://admission.bup.edu.bd",
            noticeUrl = "https://bup.edu.bd",
            aiPrepPrompt = "BUP (FST ও FBS) ভর্তি পরীক্ষার ইংরেজি প্রশ্ন, Analytical ও বিজ্ঞান প্রস্তুতির পূর্ণ রোডম্যাপ দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_jnu",
            name = "Jagannath University (JnU)",
            bnName = "জগন্নাথ বিশ্ববিদ্যালয় (জবি)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – জানুয়ারি (নিজস্ব বা GST গুচ্ছ সার্কুলার)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: ফেব্রুয়ারি – এপ্রিল।",
            eligibilityAndMarks = "• Unit-A (Science), Unit-B (Arts), Unit-C (Business): ১২০/১০০ নম্বরের লিখিত + MCQ প্যাটার্ন (সর্বশেষ সার্কুলার অনুযায়ী)।",
            registrationUrl = "https://admission.jnu.ac.bd",
            noticeUrl = "https://www.jnu.ac.bd",
            aiPrepPrompt = "জগন্নাথ বিশ্ববিদ্যালয় (JnU Unit-A) ভর্তি পরীক্ষার প্রস্তুতি ও গুরুত্বপূর্ণ টপিকগুলো বুঝিয়ে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_ku",
            name = "Khulna University (KU)",
            bnName = "খুলনা বিশ্ববিদ্যালয় (খুবি)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – ফেব্রুয়ারি (GST / নিজস্ব সার্কুলার)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – এপ্রিল।",
            eligibilityAndMarks = "• Unit-A (SET - Science, Engineering & Tech), Unit-B (Life Science): MCQ + সংক্ষিপ্ত লিখিত/GPA।",
            registrationUrl = "https://admission.ku.ac.bd",
            noticeUrl = "https://ku.ac.bd",
            aiPrepPrompt = "খুলনা বিশ্ববিদ্যালয়ের SET ও Life Science স্কুলের ভর্তি প্রস্তুতির পরামর্শ দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_gst",
            name = "GST Integrated (24+ General & Science Univs)",
            bnName = "জিএসটি গুচ্ছ ২৪+ বিশ্ববিদ্যালয় (IU, CoU, JKKNIU, BRUR, BU, রবীন্দ্র, শেখ হাসিনা...)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – ফেব্রুয়ারি (একই আবেদনে ২৪টি বিশ্ববিদ্যালয়)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – মে (Unit A: Science, B: Humanities, C: Commerce)।",
            eligibilityAndMarks = "• Unit-A (বিজ্ঞান): Physics 25, Chemistry 25 + Math/Biology/Bangla/English মিলিয়ে মোট ১০০ MCQ (নেগেটিভ ০.২৫)।",
            registrationUrl = "https://gstadmission.ac.bd",
            noticeUrl = "https://gstadmission.ac.bd",
            aiPrepPrompt = "GST গুচ্ছ ভর্তি পরীক্ষায় উচ্চ স্কোর করতে Physics, Chemistry ও Biology/Math কীভাবে প্রস্তুতি নেব?"
        ),
        AdmissionUniversityInfo(
            id = "adm_sust",
            name = "SUST (Shahjalal Univ. of Science & Tech)",
            bnName = "শাহজালাল বিজ্ঞান ও প্রযুক্তি বিশ্ববিদ্যালয় (শাবিপ্রবি - সিলেট)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – ফেব্রুয়ারি (GST / নিজস্ব)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – মে।",
            eligibilityAndMarks = "• Unit-A (Science & Engineering) এবং Unit-B (Architecture/Applied): Physics, Chemistry, Math বাধ্যতামূলক (ইঞ্জিনিয়ারিং আসনের জন্য)।",
            registrationUrl = "https://admission.sust.edu",
            noticeUrl = "https://www.sust.edu",
            aiPrepPrompt = "SUST CSE/EEE/Architecture-এ চান্স পাওয়ার জন্য প্রয়োজনীয় নম্বর ও প্রস্তুতি গাইড দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_hstu",
            name = "HSTU (Hajee Mohammad Danesh S&T Univ)",
            bnName = "হাজী মোহাম্মদ দানেশ বিজ্ঞান ও প্রযুক্তি বিশ্ববিদ্যালয় (হাবিপ্রবি - দিনাজপুর)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – ফেব্রুয়ারি (GST পোর্টাল)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – মে।",
            eligibilityAndMarks = "• Engineering, Agriculture, Veterinary (DVM), Fisheries ও General Science অনুষদে GST স্কোর ও সার্কুলার অনুযায়ী ভর্তি।",
            registrationUrl = "https://hstu.ac.bd/admission",
            noticeUrl = "https://hstu.ac.bd",
            aiPrepPrompt = "হাবিপ্রবি (HSTU)-তে Engineering ও DVM/Agriculture-এ ভর্তির সাবজেক্ট চয়েস ও যোগ্যতা বুঝিয়ে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_mbstu_just_nstu_pust",
            name = "MBSTU, JUST, NSTU & PUST (Science & Tech)",
            bnName = "মাভাবিপ্রবি (MBSTU), যবিপ্রবি (JUST), নোবিপ্রবি (NSTU) ও পাবিপ্রবি (PUST)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – মার্চ (GST গুচ্ছের অধীনে বা নিজস্ব সার্কুলার)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – মে।",
            eligibilityAndMarks = "• CSE, Pharmacy, Genetic Engineering, EEE ও Applied Chemistry আসনের জন্য Physics, Chem ও Biology/Math স্কোর দেখা হয়।",
            registrationUrl = "https://gstadmission.ac.bd",
            noticeUrl = "https://mbstu.ac.bd",
            aiPrepPrompt = "বিজ্ঞান ও প্রযুক্তি বিশ্ববিদ্যালয়গুলোর (MBSTU, JUST, NSTU, PUST) সেরা বিভাগ ও সাবজেক্ট চয়েস গাইড দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_iu_cou_brur_bu",
            name = "IU, CoU, JKKNIU, BRUR & Barishal University",
            bnName = "ইবি (IU), কুবি (CoU), জাককানইবি, বেরোবি (BRUR) ও বরিশাল বিশ্ববিদ্যালয়",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – মার্চ।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – মে।",
            eligibilityAndMarks = "• গুচ্ছ (GST) স্কোর ও বিভাগীয় শর্ত অনুযায়ী বিজ্ঞান, মানবিক ও ব্যবসায় অনুষদে ভর্তি।",
            registrationUrl = "https://gstadmission.ac.bd",
            noticeUrl = "https://www.iu.ac.bd",
            aiPrepPrompt = "IU, CoU, BRUR ও বরিশাল বিশ্ববিদ্যালয়ে ভর্তির ন্যূনতম স্কোর এবং সাবজেক্ট চয়েস সাজানোর নিয়ম দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_nu_bou",
            name = "National University (NU) & Bangladesh Open Univ (BOU)",
            bnName = "জাতীয় বিশ্ববিদ্যালয় (NU অনার্স/ডিগ্রি) ও উন্মুক্ত বিশ্ববিদ্যালয় (BOU)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – মার্চ (১ম ও ২য় রিলিজ স্লিপ)।",
            examSchedule = "সম্ভাব্য পদ্ধতি: SSC + HSC GPA ও প্রাপ্ত নম্বরের মেধাক্রম (অনার্স/প্রফেশনাল সার্কুলার অনুযায়ী)।",
            eligibilityAndMarks = "• সারাদেশের সরকারি ও বেসরকারি অনার্স কলেজে (CSE, BBA, Physics, Chemistry, English ইত্যাদি) আবেদনযোগ্য।",
            registrationUrl = "http://app1.nu.edu.bd",
            noticeUrl = "https://www.nu.ac.bd",
            aiPrepPrompt = "জাতীয় বিশ্ববিদ্যালয়ে (NU) অনার্স ও প্রফেশনাল কোর্সে কলেজ ও সাবজেক্ট পছন্দ করার সঠিক কৌশল দাও।"
        ),

        // ==================== AGRI, MARITIME, AVIATION & SPECIAL ====================
        AdmissionUniversityInfo(
            id = "adm_agri_cluster",
            name = "Agri Cluster (কৃষি গুচ্ছ ৯ বিশ্ববিদ্যালয়)",
            bnName = "সমন্বিত কৃষি বিশ্ববিদ্যালয় (BAU, SAU, BSMRAU, CVASU, PSTU, KAU, HAU...)",
            category = AdmissionCategory.AGRI_OTHER,
            circularSchedule = "সম্ভাব্য সার্কুলার: ফেব্রুয়ারি – মার্চ।",
            examSchedule = "সম্ভাব্য পরীক্ষা: এপ্রিল – মে।",
            eligibilityAndMarks = "• Biology (30), Chemistry (20), Physics (20), Math (20) ও English (10) = ১০০ MCQ + GPA ৫০ = মোট ১৫০ নম্বর।",
            registrationUrl = "https://acas.edu.bd",
            noticeUrl = "https://bau.edu.bd",
            aiPrepPrompt = "কৃষি গুচ্ছ ৯ বিশ্ববিদ্যালয়ের Biology ও Chemistry থেকে কমন পাওয়ার হাই-ইল্ড টপিকগুলো তালিকা করে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_bau_sau",
            name = "BAU (Mymensingh) & SAU (Sher-e-Bangla)",
            bnName = "বাংলাদেশ কৃষি বিশ্ববিদ্যালয় (বাকৃবি) ও শেরেবাংলা কৃষি বিশ্ববিদ্যালয় (শেকৃবি)",
            category = AdmissionCategory.AGRI_OTHER,
            circularSchedule = "সম্ভাব্য সার্কুলার: ফেব্রুয়ারি – মার্চ (কৃষি গুচ্ছ পোর্টালের মাধ্যমে)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: এপ্রিল – মে।",
            eligibilityAndMarks = "• DVM, Agriculture, Fisheries ও Ag. Engineering-এ ভর্তির জন্য Biology ও Chemistry-তে উচ্চ স্কোর আবশ্যক।",
            registrationUrl = "https://acas.edu.bd",
            noticeUrl = "https://www.sau.edu.bd",
            aiPrepPrompt = "বাকৃবি (BAU) ও শেকৃবি (SAU)-তে DVM এবং Agriculture পড়ার সুযোগ ও কাট-মার্কস সম্পর্কে বলো।"
        ),
        AdmissionUniversityInfo(
            id = "adm_maritime",
            name = "BSMRMU (Bangladesh Maritime University)",
            bnName = "বঙ্গবন্ধু শেখ মুজিবুর রহমান মেরিটাইম বিশ্ববিদ্যালয়",
            category = AdmissionCategory.AGRI_OTHER,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – ফেব্রুয়ারি।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – এপ্রিল (মেরিন ইঞ্জিনিয়ারিং, নেভাল আর্কিটেকচার, ওশানোগ্রাফি)।",
            eligibilityAndMarks = "• FET, FEOS ও FMGP অনুষদে MCQ পরীক্ষা (Math, Physics, Chemistry, English ও Maritime GK)।",
            registrationUrl = "https://applyonline.bsmrmu.edu.bd",
            noticeUrl = "https://bsmrmu.edu.bd",
            aiPrepPrompt = "BSMRMU মেরিটাইম বিশ্ববিদ্যালয়ের ভর্তি পরীক্ষার সিলেবাস ও ক্যারিয়ার সুযোগ বুঝিয়ে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_aviation",
            name = "BSMRAAU (Aviation & Aerospace University)",
            bnName = "এভিয়েশন অ্যান্ড অ্যারোস্পেস বিশ্ববিদ্যালয় (লালমনিরহাট/ঢাকা)",
            category = AdmissionCategory.AGRI_OTHER,
            circularSchedule = "সম্ভাব্য সার্কুলার: ফেব্রুয়ারি – মার্চ।",
            examSchedule = "সম্ভাব্য পরীক্ষা: এপ্রিল – মে (Aerospace & Avionics Engineering)।",
            eligibilityAndMarks = "• বিজ্ঞান বিভাগ থেকে HSC; Physics, Math, Chemistry ও English-এর ওপর ভিত্তি করে লিখিত/MCQ ও ভাইভা।",
            registrationUrl = "https://bsmraau.edu.bd",
            noticeUrl = "https://bsmraau.edu.bd",
            aiPrepPrompt = "অ্যাভিয়েশন অ্যান্ড অ্যারোস্পেস বিশ্ববিদ্যালয়ে (BSMRAAU) Aerospace Engineering ভর্তির যোগ্যতা ও প্রস্তুতি জানাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_nursing_bma",
            name = "BNMC Nursing, Midwifery & BUP Medical/Tech",
            bnName = "সরকারি ও বেসরকারি বিএসসি/ডিপ্লোমা নার্সিং (BNMC)",
            category = AdmissionCategory.AGRI_OTHER,
            circularSchedule = "সম্ভাব্য সার্কুলার: ফেব্রুয়ারি – মার্চ।",
            examSchedule = "সম্ভাব্য পরীক্ষা: এপ্রিল – মে (১০০ নম্বরের MCQ)।",
            eligibilityAndMarks = "• B.Sc in Nursing (বিজ্ঞান বিভাগ): বাংলা ২০, ইংরেজি ২০, গণিত ১০, সাধারণ বিজ্ঞান (পদার্থ, রসায়ন, জীববিজ্ঞান) ৩০ ও GK ২০ = ১০০ MCQ (নেগেটিভ নেই)।",
            registrationUrl = "http://bnmc.teletalk.com.bd",
            noticeUrl = "https://www.bnmc.gov.bd",
            aiPrepPrompt = "B.Sc নার্সিং ভর্তি পরীক্ষার বাংলা, ইংরেজি ও বিজ্ঞান অংশের সম্পূর্ণ সাজেশন দাও।"
        ),

        // ==================== PRIVATE UNIVERSITIES ====================
        AdmissionUniversityInfo(
            id = "adm_bracu",
            name = "BRAC University (BRACU)",
            bnName = "ব্র্যাক বিশ্ববিদ্যালয় (ঢাকা - মেরুল বাড্ডা)",
            category = AdmissionCategory.PRIVATE,
            circularSchedule = "সম্ভাব্য সার্কুলার: প্রতি বছরে ৩টি সেমিস্টার — Spring (অক্টোবর–ডিসেম্বর), Summer (ফেব্রুয়ারি–এপ্রিল), Fall (জুলাই–আগস্ট)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: প্রতিটি সেমিস্টার শুরুর ২–৪ সপ্তাহ আগে কম্পিউটার/লিখিত টেস্ট ও ইন্টারভিউ।",
            eligibilityAndMarks = "• SSC + HSC প্রতিটিতে ন্যূনতম GPA 3.5+ (মোট 7.0+ প্রোগ্রাম ভেদে)।\n• ভর্তি পরীক্ষা: English Grammar + Essay, Quantitative Math ও CSE/Engineering প্রোগ্রামে Higher Math + Physics।",
            registrationUrl = "https://admissions.bracu.ac.bd",
            noticeUrl = "https://www.bracu.ac.bd/admissions",
            aiPrepPrompt = "BRAC University (CSE/EEE/BBA) Admission Test-এর English Essay, Math এবং প্রোগ্রামিং প্রস্তুতির নমুনা প্রশ্ন দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_nsu",
            name = "North South University (NSU)",
            bnName = "নর্থ সাউথ বিশ্ববিদ্যালয় (বসুন্ধরা, ঢাকা)",
            category = AdmissionCategory.PRIVATE,
            circularSchedule = "সম্ভাব্য সার্কুলার: Spring, Summer ও Fall (বছরে ৩টি ইনটেক)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি, মে এবং সেপ্টেম্বর (অনলাইন আবেদন শেষে নির্ধারিত তারিখে)।",
            eligibilityAndMarks = "• SSC + HSC মোট GPA ≥ 7.0 (আলাদা ≥ 3.5)। ফার্মেসি (B.Pharm)-এর জন্য Biology ও Chemistry জরুরি।\n• টেস্ট: English Reading & Writing + Quantitative Aptitude (Math)।",
            registrationUrl = "https://admissions.northsouth.edu",
            noticeUrl = "https://www.northsouth.edu",
            aiPrepPrompt = "NSU Admission Test-এর English Comprehension, Grammar ও Math অংশে ভালো করার প্রস্তুতি গাইড দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_aust",
            name = "AUST (Ahsanullah Univ. of Science & Tech)",
            bnName = "আহছানউল্লা বিজ্ঞান ও প্রযুক্তি বিশ্ববিদ্যালয় (তেজগাঁও, ঢাকা)",
            category = AdmissionCategory.PRIVATE,
            circularSchedule = "সম্ভাব্য সার্কুলার: Spring (ফেব্রুয়ারি) ও Fall (আগস্ট) — বছরে ২টি সেমিস্টার।",
            examSchedule = "সম্ভাব্য পরীক্ষা: সার্কুলার অনুযায়ী ভর্তি পরীক্ষা ও মেধাতালিকা।",
            eligibilityAndMarks = "• ইঞ্জিনিয়ারিং (CSE, EEE, Civil, ME, TE, IPE): বিজ্ঞান বিভাগ, Physics/Math/Chemistry ভালো গ্রেড প্রয়োজন।\n• পরীক্ষা: Physics, Chemistry, Math ও English MCQ/লিখিত।",
            registrationUrl = "https://admission.aust.edu",
            noticeUrl = "https://www.aust.edu",
            aiPrepPrompt = "AUST ইঞ্জিনিয়ারিং ভর্তি পরীক্ষার মানবণ্টন ও Physics/Math প্রস্তুতির সেরা টিপস দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_iub_ewu",
            name = "IUB & East West University (EWU)",
            bnName = "ইন্ডিপেন্ডেন্ট (IUB) ও ইস্ট ওয়েস্ট বিশ্ববিদ্যালয় (EWU)",
            category = AdmissionCategory.PRIVATE,
            circularSchedule = "সম্ভাব্য সার্কুলার: Spring, Summer ও Fall সেমিস্টার।",
            examSchedule = "সম্ভাব্য পরীক্ষা: প্রতিটি ইনটেকের ২–৩ সপ্তাহ আগে (স্কলারশিপের সুযোগসহ)।",
            eligibilityAndMarks = "• SSC + HSC প্রতিটিতে ন্যূনতম GPA 3.0–3.5।\n• English, General Math ও Analytical MCQ + Writing পরীক্ষা।",
            registrationUrl = "https://admission.ewubd.edu",
            noticeUrl = "https://www.iub.edu.bd",
            aiPrepPrompt = "IUB ও EWU ভর্তি পরীক্ষা এবং Merit Scholarship পাওয়ার নিয়ম ও প্রস্তুতি বুঝিয়ে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_aiub_uiu_diu",
            name = "AIUB, UIU & Daffodil International Univ (DIU)",
            bnName = "এআইইউবি (AIUB), ইউআইইউ (UIU) ও ড্যাফোডিল (DIU)",
            category = AdmissionCategory.PRIVATE,
            circularSchedule = "সম্ভাব্য সার্কুলার: Spring, Summer ও Fall (সারা বছর নির্ধারিত ইনটেক)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: আবেদন করার পর নির্ধারিত স্লটে পরীক্ষা ও ফলাফল।",
            eligibilityAndMarks = "• CSE/EEE/SWE/BBA: SSC+HSC প্রতিটিতে ≥ 2.5–3.0। Golden GPA 5.0-এ ১০০% পর্যন্ত মেধাবৃত্তি সুযোগ।",
            registrationUrl = "https://www.aiub.edu/admission",
            noticeUrl = "https://admission.uiu.ac.bd",
            aiPrepPrompt = "AIUB, UIU ও DIU-তে CSE/Software Engineering ভর্তি যোগ্যতা, স্কলারশিপ ও পরীক্ষার নমুনা প্রশ্ন দাও।"
        )
    )
}
