package com.intisarmuhib.teachsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final List<StudentModel> studentList;
    private final Map<String, String> attendanceMap;

    public AttendanceAdapter(List<StudentModel> studentList, Map<String, String> attendanceMap) {
        this.studentList = studentList;
        this.attendanceMap = attendanceMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentModel student = studentList.get(position);
        holder.tvName.setText(student.getName());

        String status = attendanceMap.get(student.getId());
        if (status == null) status = "Present"; // Default

        if ("Present".equals(status)) holder.rbPresent.setChecked(true);
        else if ("Absent".equals(status)) holder.rbAbsent.setChecked(true);
        else if ("Late".equals(status)) holder.rbLate.setChecked(true);
        else if ("Leave".equals(status)) holder.rbLeave.setChecked(true);

        holder.rgAttendance.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPresent) attendanceMap.put(student.getId(), "Present");
            else if (checkedId == R.id.rbAbsent) attendanceMap.put(student.getId(), "Absent");
            else if (checkedId == R.id.rbLate) attendanceMap.put(student.getId(), "Late");
            else if (checkedId == R.id.rbLeave) attendanceMap.put(student.getId(), "Leave");
        });
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        RadioGroup rgAttendance;
        RadioButton rbPresent, rbAbsent, rbLate, rbLeave;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            rgAttendance = itemView.findViewById(R.id.rgAttendance);
            rbPresent = itemView.findViewById(R.id.rbPresent);
            rbAbsent = itemView.findViewById(R.id.rbAbsent);
            rbLate = itemView.findViewById(R.id.rbLate);
            rbLeave = itemView.findViewById(R.id.rbLeave);
        }
    }
}
