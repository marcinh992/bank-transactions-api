package com.marcinpypec.banktransactions.importjob.mapper;

import com.marcinpypec.banktransactions.importjob.dto.ImportJobResponse;
import com.marcinpypec.banktransactions.importjob.model.ImportJobDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImportJobMapper {

    ImportJobResponse toResponse(ImportJobDocument job);

}