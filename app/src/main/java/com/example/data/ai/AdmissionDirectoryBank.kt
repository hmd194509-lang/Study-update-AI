package com.example.data.ai

import com.example.data.model.AdmissionCategory
import com.example.data.model.AdmissionUniversityInfo

object AdmissionDirectoryBank {

    val entries: List<AdmissionUniversityInfo> = listOf(
        AdmissionUniversityInfo(
            id = "adm_medical",
            name = "MBBS & BDS (Medical & Dental)",
            bnName = "সরকারি ও বেসরকারি মেডিকেল ও ডেন্টাল ভর্তি",
            category = AdmissionCategory.MEDICAL,
            circularSchedule = "সম্ভাব্য সার্কুলার: HSC ফলাফল প্রকাশের ৩–৪ সপ্তাহ পর (ডিসেম্বর – জানুয়ারি)। আবেদন চলে প্রায় ২–৩ সপ্তাহ।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – ফেব্রুয়ারি (MBBS), মার্চ – এপ্রিল (BDS)।",
            eligibilityAndMarks = "• SSC + HSC মোট GPA ≥ 9.0 (Biology-তে কমপক্ষে GPA 4.0)\n• লিখিত MCQ: ১০০ নম্বর (Biology 30, Chemistry 25, Physics 20, English 15, GK 10) + GPA 100 নম্বর। নেগেটিভ মার্কিং: ০.২৫।",
            registrationUrl = "https://dgme.teletalk.com.bd",
            noticeUrl = "https://dgme.gov.bd",
            aiPrepPrompt = "মেডিকেল (MBBS) অ্যাডমিশন পরীক্ষার সর্বশেষ প্রস্তুতি কৌশল, কোন অধ্যায় থেকে সবচেয়ে বেশি প্রশ্ন আসে এবং GPA + নেগেটিভ মার্কিং হিসাব বিস্তারিত বুঝিয়ে দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_buet",
            name = "BUET Undergraduate Admission",
            bnName = "বাংলাদেশ প্রকৌশল বিশ্ববিদ্যালয় (বুয়েট)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: নভেম্বর – জানুয়ারি। প্রথমে প্রিলিমিনারি আবেদন ও শর্টলিস্ট প্রকাশ করা হয়।",
            examSchedule = "সম্ভাব্য পরীক্ষা: প্রিলিমিনারি (জানুয়ারি/ফেব্রুয়ারি) এবং চূড়ান্ত লিখিত পরীক্ষা (ফেব্রুয়ারি/মার্চ)।",
            eligibilityAndMarks = "• HSC-তে Physics, Chemistry ও Higher Math-এ উচ্চ নম্বর ও নির্ধারিত GPA প্রয়োজন।\n• প্রিলিমিনারি (১০০ MCQ) শেষে নির্বাচিতরা চূড়ান্ত লিখিত (৪০০ নম্বর: Math, Physics, Chemistry) পরীক্ষা দেয়।",
            registrationUrl = "https://ugadmission.buet.ac.bd",
            noticeUrl = "https://www.buet.ac.bd",
            aiPrepPrompt = "BUET প্রিলিমিনারি ও লিখিত পরীক্ষার প্রস্তুতির জন্য Physics, Chemistry ও Math-এর কোন অধ্যায়গুলো সবচেয়ে গুরুত্বপূর্ণ ও সময় ব্যবস্থাপনা কেমন হওয়া উচিত?"
        ),
        AdmissionUniversityInfo(
            id = "adm_du_ka",
            name = "Dhaka University 'Ka' (Science Unit)",
            bnName = "ঢাকা বিশ্ববিদ্যালয় বিজ্ঞান ইউনিট ('ক')",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: নভেম্বর – ডিসেম্বর (আবেদন চলে প্রায় ৩–৪ সপ্তাহ)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – ফেব্রুয়ারি (MCQ + Written সমন্বিত পরীক্ষা)।",
            eligibilityAndMarks = "• SSC + HSC মোট GPA ≥ 8.0 (আলাদাভাবে ন্যূনতম ৩.৫)।\n• মোট ১২০ নম্বর: MCQ ৬০ + লিখিত ৪০ + GPA ২০ (২য় বারের সুযোগ নেই)।",
            registrationUrl = "https://admission.eis.du.ac.bd",
            noticeUrl = "https://www.du.ac.bd",
            aiPrepPrompt = "ঢাকা বিশ্ববিদ্যালয় 'ক' (বিজ্ঞান) ইউনিটের MCQ ৬০ এবং লিখিত ৪০ নম্বরের জন্য Physics, Chemistry, Math ও Biology প্রস্তুতির সেরা গাইডলাইন দাও।"
        ),
        AdmissionUniversityInfo(
            id = "adm_ckruet",
            name = "CKRUET (CUET, KUET & RUET)",
            bnName = "প্রকৌশল গুচ্ছ (চুয়েট, কুয়েট, রুয়েট)",
            category = AdmissionCategory.ENGINEERING,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – জানুয়ারি (একত্রে অথবা নিজস্ব পোর্টালে আলাদা বিজ্ঞপ্তি)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – মার্চ (৫০০ নম্বরের MCQ পরীক্ষা, গ্রুপ 'ক' ও 'খ')।",
            eligibilityAndMarks = "• Physics, Chemistry, Higher Math ও English মিলে শর্টলিস্ট ও আবেদন যোগ্যতা নির্ধারিত হয়।\n• পরীক্ষা: Math, Physics, Chemistry ও English মোট ৫০০ নম্বর (আর্কিটেকচারের জন্য অঙ্কনসহ)।",
            registrationUrl = "https://admissionckruet.ac.bd",
            noticeUrl = "https://www.cuet.ac.bd",
            aiPrepPrompt = "CUET, KUET ও RUET ইঞ্জিনিয়ারিং ভর্তি পরীক্ষার প্রশ্নপ্যাটার্ন, ক্যালকুলেটর নিয়ম ও উচ্চতর ম্যাথ/ফিজিক্স শর্টকাট কৌশল আলোচনা করো।"
        ),
        AdmissionUniversityInfo(
            id = "adm_gst",
            name = "GST Integrated (24+ Universities)",
            bnName = "জিএসটি গুচ্ছভুক্ত ২৪+ সাধারণ ও বিজ্ঞান বিশ্ববিদ্যালয়",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: জানুয়ারি – ফেব্রুয়ারি (SUST, JNU, CoU, JUST, MBSTU, NSTU সহ গুচ্ছ বিশ্ববিদ্যালয়)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: মার্চ – মে (Unit A: Science, Unit B: Humanities, Unit C: Business)।",
            eligibilityAndMarks = "• Unit-A (বিজ্ঞান): SSC + HSC GPA ≥ 7.5–8.0 (সর্বশেষ সার্কুলার অনুযায়ী)।\n• ১০০ নম্বরের MCQ (Physics, Chemistry বাধ্যতামূলক + Math/Biology/Bangla/English)।",
            registrationUrl = "https://gstadmission.ac.bd",
            noticeUrl = "https://gstadmission.ac.bd",
            aiPrepPrompt = "GST গুচ্ছ ভর্তি পরীক্ষার (Unit-A বিজ্ঞান) মানবণ্টন, কোন সাবজেক্ট দাগানো বুদ্ধিমানের কাজ এবং ২য় বারের পরীক্ষার্থীদের করণীয় কী?"
        ),
        AdmissionUniversityInfo(
            id = "adm_agri",
            name = "Agri Cluster (কৃষি গুচ্ছ ৯ বিশ্ববিদ্যালয়)",
            bnName = "সমন্বিত কৃষি বিশ্ববিদ্যালয় গুচ্ছ (BAU, BSMRAU, SAU, CVASU)",
            category = AdmissionCategory.AGRI_OTHER,
            circularSchedule = "সম্ভাব্য সার্কুলার: ফেব্রুয়ারি – মার্চ।",
            examSchedule = "সম্ভাব্য পরীক্ষা: এপ্রিল – মে।",
            eligibilityAndMarks = "• Biology, Chemistry, Physics, Math ও English মিলিয়ে ১০০ MCQ + GPA ৫০ নম্বর।\n• জীববিজ্ঞান ও রসায়ন সবচেয়ে বেশি গুরুত্ব বহন করে।",
            registrationUrl = "https://acas.edu.bd",
            noticeUrl = "https://bau.edu.bd",
            aiPrepPrompt = "কৃষি গুচ্ছ (Agri Cluster) ভর্তি পরীক্ষার জন্য Biology, Chemistry ও Physics-এর কোন বিষয়গুলোতে জোর দিলে চান্স নিশ্চিত করা যায়?"
        ),
        AdmissionUniversityInfo(
            id = "adm_ru_cu_ju",
            name = "RU, CU, JU & BUP Admissions",
            bnName = "রাবি (RU), চবি (CU), জাবি (JU) ও বিইউপি (BUP)",
            category = AdmissionCategory.UNIVERSITY,
            circularSchedule = "সম্ভাব্য সার্কুলার: ডিসেম্বর – ফেব্রুয়ারি (প্রতিটি বিশ্ববিদ্যালয়ের নিজস্ব পোর্টালে আলাদাভাবে প্রকাশিত হয়)।",
            examSchedule = "সম্ভাব্য পরীক্ষা: জানুয়ারি – এপ্রিল (শিফট ও ইউনিট ভিত্তিক পরীক্ষা)।",
            eligibilityAndMarks = "• BUP সাধারণত সবার আগে জানুয়ারিতে পরীক্ষা নেয় (ইংরেজি মাধ্যম ও দ্রুত MCQ)।\n• RU, CU ও JU-তে ২য় বার পরীক্ষার সুযোগ ও শর্ত প্রতি বছরের সার্কুলারে উল্লেখ থাকে।",
            registrationUrl = "https://admission.bup.edu.bd",
            noticeUrl = "https://admission.ru.ac.bd",
            aiPrepPrompt = "RU, CU, JU এবং BUP-এর বিজ্ঞান ইউনিটের ভর্তি পরীক্ষার ধরন ও ২য় বার (Second Timer) প্রার্থীদের সেরা প্রস্তুতি প্ল্যান সাজিয়ে দাও।"
        )
    )
}
