package com.vdms.web;

import java.util.List;

public record ApiError(int status, String message, List<String> details) {
}
