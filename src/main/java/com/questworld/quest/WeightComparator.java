package com.questworld.quest;

import com.questworld.api.contract.IQuest;
import java.util.Comparator;

public class WeightComparator implements Comparator<IQuest> {

  @Override
  public int compare(IQuest q1, IQuest q2) {
    int w1 = q1.getWeight();
    while (q1.getParent() != null && w1 == 0) {
      q1 = q1.getParent();
      w1 = q1.getWeight();
    }
    int w2 = q2.getWeight();
    while (q2.getParent() != null && w2 == 0) {
      q2 = q2.getParent();
      w2 = q2.getWeight();
    }
    return w1 - w2;
  }

}
