package aggression

import (
	"reflect"
	"testing"
)

func TestAddSession(t *testing.T) {
	state := userStateData{}
	{
		state.AddSession(sessionScoringRow{
			SessionID: "1", StartTime: 1, FinishTime: 5, Mileage: 30,
		})
		answer1 := []scoredSession{
			{SessionID: "1", FinishTime: 5, Mileage: 30},
		}
		if !reflect.DeepEqual(state.ScoredSessions, answer1) {
			t.Fatalf("Expected %v, got %v", answer1, state.ScoredSessions)
		}
	}
	{
		state.AddSession(sessionScoringRow{
			SessionID: "2", StartTime: 6, FinishTime: 15, Mileage: 180,
		})
		answer2 := []scoredSession{
			{SessionID: "1", FinishTime: 5, Mileage: 30},
			{SessionID: "2", FinishTime: 15, Mileage: 180},
		}
		if !reflect.DeepEqual(state.ScoredSessions, answer2) {
			t.Fatalf("Expected %v, got %v", answer2, state.ScoredSessions)
		}
	}
	{
		state.AddSession(sessionScoringRow{
			SessionID: "5", StartTime: 30, FinishTime: 35, Mileage: 30,
		})
		answer3 := []scoredSession{
			{SessionID: "2", FinishTime: 15, Mileage: 180},
			{SessionID: "5", FinishTime: 35, Mileage: 30},
		}
		if !reflect.DeepEqual(state.ScoredSessions, answer3) {
			t.Fatalf("Expected %v, got %v", answer3, state.ScoredSessions)
		}
	}
	{
		state.AddSession(sessionScoringRow{
			SessionID: "4", StartTime: 28, FinishTime: 30, Mileage: 30,
		})
		answer4 := []scoredSession{
			{SessionID: "2", FinishTime: 15, Mileage: 180},
			{SessionID: "4", FinishTime: 30, Mileage: 30},
			{SessionID: "5", FinishTime: 35, Mileage: 30},
		}
		if !reflect.DeepEqual(state.ScoredSessions, answer4) {
			t.Fatalf("Expected %v, got %v", answer4, state.ScoredSessions)
		}
	}
	{
		state.AddSession(sessionScoringRow{
			SessionID: "5", StartTime: 30, FinishTime: 35, Mileage: 30,
		})
		answer5 := []scoredSession{
			{SessionID: "2", FinishTime: 15, Mileage: 180},
			{SessionID: "4", FinishTime: 30, Mileage: 30},
			{SessionID: "5", FinishTime: 35, Mileage: 30},
		}
		if !reflect.DeepEqual(state.ScoredSessions, answer5) {
			t.Fatalf("Expected %v, got %v", answer5, state.ScoredSessions)
		}
	}
	{
		state.AddSession(sessionScoringRow{
			SessionID: "6", StartTime: 34, FinishTime: 35, Mileage: 30,
		})
		answer6 := []scoredSession{
			{SessionID: "2", FinishTime: 15, Mileage: 180},
			{SessionID: "4", FinishTime: 30, Mileage: 30},
			{SessionID: "5", FinishTime: 35, Mileage: 30},
			{SessionID: "6", FinishTime: 35, Mileage: 30},
		}
		if !reflect.DeepEqual(state.ScoredSessions, answer6) {
			t.Fatalf("Expected %v, got %v", answer6, state.ScoredSessions)
		}
	}
	{
		state.AddSession(sessionScoringRow{
			SessionID: "5", StartTime: 30, FinishTime: 35, Mileage: 30,
		})
		answer6 := []scoredSession{
			{SessionID: "2", FinishTime: 15, Mileage: 180},
			{SessionID: "4", FinishTime: 30, Mileage: 30},
			{SessionID: "5", FinishTime: 35, Mileage: 30},
			{SessionID: "6", FinishTime: 35, Mileage: 30},
		}
		if !reflect.DeepEqual(state.ScoredSessions, answer6) {
			t.Fatalf("Expected %v, got %v", answer6, state.ScoredSessions)
		}
	}
}
