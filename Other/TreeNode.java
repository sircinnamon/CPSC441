/*
 * CPSC411 Assignment 2 W15
 *
 * James Kemper | 10078256
 * Feb 23, 2015
 *
 * References:
 *     - The assignment description page
*/

import java.util.List;
import java.util.ArrayList;

public class TreeNode <T>{

	private T data;
	private TreeNode<T> parent;
	private List<TreeNode<T>> children;

	public TreeNode(T data){
		this.data = data;
		this.children = new ArrayList<TreeNode<T>>();
	}

	public TreeNode<T> addChild(T data){
		TreeNode<T> node = new TreeNode<T>(data);
		node.parent = this;
		node.data = data;
		children.add(node);

		return node;
	}

	public boolean removeChild(TreeNode<T> node){
		if(children.contains(node)){
			children.remove(node);
			return true;
		}
		return false;
	}

	public List<TreeNode<T>> listChildren(){
		return children;
	}

	public T getData(){
		return data;
	}

	public void setData(T data){
		this.data = data;
	}

	public void printTree(){
		System.out.print(data.toString() + " ");
		List<TreeNode<T>> children = listChildren();
		if(children.size() > 0)
			System.out.print("( ");
		for(TreeNode<T> child : children){
			child.printTree();
		}
		if(children.size() > 0)
			System.out.print(") ");
	}

	public void cleanUp(){
		// if(parent != null){
		// 	System.out.println("loop, parent = " + parent.data.toString());
		// 	System.out.println(parent.data.toString().compareTo("unset"));
		// }
		List<TreeNode<T>> children = listChildren();
		if(children.size() > 0)
			for(TreeNode<T> child : children){
				child.cleanUp();
				if(data.toString().compareTo("unset") == 0){
					System.out.println("FOUND ONE! " + parent.data.toString());
					child.parent = parent;
				}
			}
			if(data.toString().compareTo("unset") == 0){
				parent.children.remove(this);
				parent.children.addAll(children);
			}
	}
}