package team6.car.member.service;

import org.springframework.http.ResponseEntity;
import team6.car.member.DTO.*;
import team6.car.member.domain.Complaint;
import team6.car.member.domain.Member;
import team6.car.member.response.Message;

import java.util.List;

public interface MemberService {
    Member register(UserDto userDto);
    Complaint report(ReportDto reportDto) throws Exception;
    getReportDto getReportInfo(Long id) throws Exception;
}
