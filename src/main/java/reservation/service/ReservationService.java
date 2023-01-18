package reservation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reservation.model.domain.Reservation;
import reservation.model.domain.Theme;
import reservation.model.dto.ReservationRequest;
import reservation.model.dto.ReservationResponse;
import reservation.respository.ThemeJdbcTemplateRepository;
import reservation.util.exception.restAPI.DuplicateException;
import reservation.respository.ReservationJdbcTemplateRepository;
import reservation.util.exception.restAPI.NotFoundException;

import static reservation.util.exception.ErrorMessages.*;

@Service
public class ReservationService {
    private final ReservationJdbcTemplateRepository reservationJdbcTemplateRepository;
    private final ThemeJdbcTemplateRepository themeJdbcTemplateRepository;

    @Autowired
    public ReservationService(ReservationJdbcTemplateRepository reservationJdbcTemplateRepository, ThemeJdbcTemplateRepository themeJdbcTemplateRepository) {
        this.reservationJdbcTemplateRepository = reservationJdbcTemplateRepository;
        this.themeJdbcTemplateRepository = themeJdbcTemplateRepository;
    }

    @Transactional
    public Long createReservation(ReservationRequest reservationRequest) {
        // 예약하려고 하는 테마가 존재하는지 확인
        if(!themeJdbcTemplateRepository.checkExistById(reservationRequest.getThemeId())){
            throw new NotFoundException(THEME_NOT_FOUND);
        }

        // 같은 시간에 같은 테마로 예약된 예약이 있는지 확인
        if(reservationJdbcTemplateRepository.existByDateTimeTheme(
                reservationRequest.getDate(), reservationRequest.getTime(), reservationRequest.getThemeId())){
            throw new DuplicateException(RESERVATION_DUPLICATED);
        }

        return reservationJdbcTemplateRepository.save(changeToReservation(reservationRequest));
    }

    @Transactional
    public ReservationResponse getReservation(Long id) {
        if(!reservationJdbcTemplateRepository.existById(id)){
            throw new NotFoundException(RESERVATION_NOT_FOUND);
        }

        Reservation reservation = reservationJdbcTemplateRepository.findById(id);
        Theme theme = themeJdbcTemplateRepository.findById(id);

        return changeToResponseReservation(reservation, theme);
    }

    @Transactional
    public void deleteReservation(Long id) {
        if(!reservationJdbcTemplateRepository.existById(id)){
            throw new NotFoundException(RESERVATION_NOT_FOUND);
        }
        reservationJdbcTemplateRepository.deleteById(id);
    }

    // 저장되기 이전의 Reservation 객체를 생성 - 레이어 간 DTO 구분
    private Reservation changeToReservation(ReservationRequest req) {
        return new Reservation(0L, req.getDate(), req.getTime(), req.getName(), req.getThemeId());
    }

    // 반환하기 이전의 Response 객체 생성
    private ReservationResponse changeToResponseReservation(Reservation reservation, Theme theme){
        return new ReservationResponse(
                reservation.getId(), reservation.getDate(),
                reservation.getTime(), reservation.getName(), theme);
    }
}
