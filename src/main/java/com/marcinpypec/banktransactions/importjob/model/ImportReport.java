package com.marcinpypec.banktransactions.importjob.model;

public record ImportReport(int totalRows, int importedRows, int invalidRows) {
    public static ImportReport empty() { return new ImportReport(0,0,0); }

    public ImportReport incTotal()    { return new ImportReport(totalRows + 1, importedRows, invalidRows); }
    public ImportReport incImported() { return new ImportReport(totalRows, importedRows + 1, invalidRows); }
    public ImportReport incInvalid()  { return new ImportReport(totalRows, importedRows, invalidRows + 1); }
}
