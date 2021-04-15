package tasks;

public class TreeNode {

	public TreeNode parent;
	public TreeNode a1;
	public TreeNode a2;
	public DeliveryTask bt;
	
	public boolean child1;
	
	public int tasksBegin;
	public int tasksEnd;
	
	
	public TreeNode(TreeNode parent, DeliveryTask bt, int tasksBegin, int tasksEnd, boolean child1) {
		this.parent=parent;
		this.bt=bt;
		this.tasksBegin= tasksBegin;
		this.tasksEnd = tasksEnd;
		this.child1=child1;
	}
	
	public TreeNode getChild1() {
		return a1;
	}
	
	public TreeNode getChild2() {
		return a2;
	}
	
	public TreeNode getParent() {
		return parent;
	}
	
	public void generateChilderen(DeliveryTask bt, int indexBegin, int indexEnd) {
		if(tasksBegin > indexBegin) {
			a1 = new TreeNode(this, bt, tasksBegin, tasksEnd, true);
		}else {
			a1 = new TreeNode(this, bt, indexBegin, tasksEnd, true);
		}
		
		if(tasksEnd > indexEnd ) {
			a2 = new TreeNode(this, bt, tasksBegin, tasksEnd, false);
		} else {
			a2 = new TreeNode(this, bt, tasksBegin, indexEnd, false);
		}
	}
	
	public int getTotalTasks(int size) {
		return tasksBegin + tasksEnd;
	}
}
