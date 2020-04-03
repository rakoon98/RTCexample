package com.example.mzrtc.rtc.data

import java.util.regex.Pattern

// Regex pattern used for checking if room id looks like an IP.
val IP_PATTERN = Pattern.compile(
    "(" // IPv4
            + "((\\d+\\.){3}\\d+)|" // IPv6
            + "\\[((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::"
            + "(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)\\]|"
            + "\\[(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})\\]|" // IPv6 without []
            + "((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)|"
            + "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})|" // Literals
            + "localhost"
            + ")" // Optional port number
            + "(:(\\d+))?"
)