package com.healthmanage.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.healthmanage.config.EnvConfig;
import com.healthmanage.dao.AttDAO;
import com.healthmanage.model.Attendance;
import com.healthmanage.utils.Time;
import com.healthmanage.view.View;

public class AttendanceService {
    private View view;
    private static AttendanceService instance;
    public static Map<String, List<Attendance>> attendanceList;//user 의 출근시간 기록
    private Time time;
    private LogService logger;
    private AttDAO attDAO;

    public AttendanceService() {
        view = new View();
        time = Time.getInstance();
        logger = LogService.getInstance();
        attDAO = new AttDAO();
        load();
    }

    public static AttendanceService getInstance() {
        if (instance == null) {
            instance = new AttendanceService();
        }
        return instance;
    }

    //출근 시간 기록
    public void setEnterTime(String userId){
        String date = time.currentDay();
        String enterTime = time.currentDayAndTime();
        Attendance attendance = new Attendance(userId, date, enterTime);

        //user 의 Attendance 객체를 리스트에 담기
        attendanceList.putIfAbsent(userId, new ArrayList<Attendance>()); // 기존에 없으면 새로운 리스트 생성-맵에 추가
        attendanceList.get(userId).add(attendance);
        logger.addLog(userId+"님이"+ date + enterTime+"에 입장하셨습니다.");
    }
    
    public void load() {
    	attDAO.loadAtts(EnvConfig.get("ATT_FILE"));
		logger.addLog(EnvConfig.get("ATT_FILE") + " File LOAD");
    }
    
    public void save() {
    	attDAO.saveAtts();
		logger.addLog(EnvConfig.get("ATT_FILE") + " File SAVE");
	}

    //퇴근 시간 기록
    public void setLeaveTime(String userId){
        List<Attendance> userAttendanceList = attendanceList.get(userId);
        if(userAttendanceList == null || userAttendanceList.isEmpty()){
            view.showMessage("입장 기록이 없습니다.");
            view.showMessage("입장을 먼저 해주세요!");
        }
        if(!Objects.equals(userAttendanceList.get(userAttendanceList.size() - 1).getLeaveTime(), "")){
            view.showMessage("입장 기록이 없습니다.");
            view.showMessage("입장을 먼저 해주세요!");
        }

        String leaveTime = time.currentDayAndTime();
        Attendance lastAttendance = userAttendanceList.get(userAttendanceList.size()-1);
        lastAttendance.setLeaveTime(leaveTime);

        // 입장-퇴장 시간 차이 계산
        Duration diffTime = time.getTimeDiff(lastAttendance.getEnterTime(), leaveTime);
        String diffTimeStr = time.transTimeFormat(diffTime);
        lastAttendance.setWorkOutTime(diffTimeStr);
        
        logger.addLog(userId+"님이"+ leaveTime+"에 퇴장하셨습니다.");
    }

    //일별 개인 입/퇴장 기록 조회
    public String getAttendacneByDay(String userId, String date){
        List<Attendance> userAttendanceList = attendanceList.get(userId);
        String attendacneByDay = "";
        if (userAttendanceList != null) {
            for (Attendance attendance : userAttendanceList) {
                if(attendance.getDate().equals(date)){
                    attendacneByDay = attendance.toStringAttendacne();
                }
            }
        }else{
            return "기록이 없습니다.";
        }
        return attendacneByDay;
    }

    //전체 출결 불러오기(날짜 별)
    public HashMap<String, String> listAllAttendanceByDay(String date) {
        HashMap<String, String> map = new HashMap<>();

        for (String userId : attendanceList.keySet()) {
            String attendanceByDay = getAttendacneByDay(userId, date);
            if(!attendanceByDay.isEmpty()){
                map.put(userId, attendanceByDay);
            }
        }
        return map;
    }

    //개인 회원 출결 조회 (전체)
    public List<String> listUserAttendaceAll(String userId) {
        List<Attendance> userattendanceList = attendanceList.get(userId);
        List<String> attendanceList = new ArrayList<>();
        for(int i = 0; i < userattendanceList.size(); i++){
            attendanceList.add("[" + userattendanceList.get(i).getDate() + "] " + userattendanceList.get(i).toStringAttendacne());
        }
        return attendanceList;
    }

    //전체 운동시간 기록 조회(전체 출결 조회)
    public void listAttendanceAll(String userId){
        List<Attendance> userAttendanceList = attendanceList.get(userId);
        view.showMessage("[전체 운동시간 기록]");

        if(userAttendanceList == null || userAttendanceList.isEmpty()){
            view.showMessage("아직 운동기록이 없습니다.");
        }else{
            for(Attendance attendance : userAttendanceList){
                view.showMessage(attendance.toStringWorkOut());
            }
        }
    }

    //전체 누적 운동시간 조회
    public String getTotalWorkOutTime(String userId){
        List<Attendance> userAttendanceList = attendanceList.get(userId);
        Duration totalDuration = Duration.ZERO;
        if (userAttendanceList != null) {
            for (Attendance attendance : userAttendanceList) {
                String workOutTime = attendance.getWorkOutTime(); // "HH:mm:ss" 형식의 문자열
                totalDuration = totalDuration.plus(time.totalDuration(workOutTime));
            }
        }

        return time.transTimeFormat(totalDuration);
    }

    //단일(날짜 선택) 운동시간 조회
    public String getDayWorkOutTime(String userId, String day){
        List<Attendance> userAttendanceList = attendanceList.get(userId);
        Duration totalDuration = Duration.ZERO;  // 운동 시간 합산용 변수
        String dayWorkOutTime = "";

        if (userAttendanceList != null) {
            for (Attendance attendance : userAttendanceList) {
                if(attendance.getDate().equals(day)) {
                    // 운동 시간 문자열을 Duration으로 변환하여 누적
                    Duration workOutDuration = time.totalDuration(attendance.getWorkOutTime());
                    totalDuration = totalDuration.plus(workOutDuration);
                }
            }
        }
        // 최종 누적된 운동 시간을 문자열로 변환
        dayWorkOutTime = time.transTimeFormat(totalDuration);

        return dayWorkOutTime;
    }

    // 월별 누적 운동시간 조회
    public String getMonthTotalWorkOutTime(String userId, String yearMonth) {
        List<Attendance> userAttendanceList = attendanceList.get(userId);
        Duration totalDuration = Duration.ZERO; // 누적 시간 초기화

        if (userAttendanceList != null) {
            for (Attendance attendance : userAttendanceList) {
                // 월이 일치하는지 확인
                if (time.getYearMonthByInput(attendance.getDate()).equals(yearMonth)) {
                    // 월별 운동 시간을 누적
                    totalDuration = totalDuration.plus(time.totalDuration(attendance.getWorkOutTime()));
                }
            }
        }
        // 누적된 시간을 "HH:mm:ss" 형식으로 변환하여 반환
        return time.transTimeFormat(totalDuration);
    }

}
